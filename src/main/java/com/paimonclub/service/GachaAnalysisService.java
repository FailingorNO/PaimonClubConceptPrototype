package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.common.PageResult;
import com.paimonclub.dao.GachaRecordDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.GachaRecord;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * 抽卡分析服务：抽卡记录导入（模拟/手工）、概率与保底分析、欧非曲线、卡池对比。
 */
@ApplicationScoped
public class GachaAnalysisService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String[] FIVE_STAR = {"神里绫华", "胡桃", "雷电将军", "纳西妲", "夜兰", "申鹤", "妮露", "赛诺"};
    private static final String[] FOUR_STAR = {"行秋", "香菱", "北斗", "凝光", "班尼特", "重云", "菲谢尔", "罗莎莉亚"};
    private static final String[] THREE_STAR = {"飞天御剑", "以理服人", "神射手之誓", "翡玉法球", "黑缨枪", "鸦羽弓"};

    @Inject
    GachaRecordDao gachaRecordDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject VoMapper mapper;

    /** 导入抽卡记录：records 为空时按 mockCount 生成模拟数据 */
    @Transactional
    public int importRecords(Reqs.GachaImport req) {
        Long userId = currentUser.requireLogin();
        String poolName = req.poolName == null || req.poolName.isBlank() ? "角色活动祈愿" : req.poolName.trim();
        List<Reqs.GachaImport.Item> items = req.records;
        if (items == null || items.isEmpty()) {
            items = mockItems(180);
        }
        if (items.size() > 2000) {
            throw BusinessException.badRequest("单次导入不能超过 2000 条记录");
        }
        int saved = 0;
        LocalDateTime base = LocalDateTime.now().minusDays(30);
        for (int i = 0; i < items.size(); i++) {
            Reqs.GachaImport.Item item = items.get(i);
            GachaRecord record = new GachaRecord();
            record.setUserId(userId);
            record.setGameUid(req.gameUid);
            record.setPoolName(poolName);
            record.setItemName(item.itemName == null || item.itemName.isBlank() ? "未知" : item.itemName.trim());
            record.setItemType(item.itemType == null || item.itemType.isBlank() ? "角色" : item.itemType.trim());
            int rarity = item.rarity == null ? 3 : item.rarity;
            record.setRarity(Math.min(5, Math.max(3, rarity)));
            record.setPullTime(parseTime(item.pullTime, base.plusMinutes(i * 7L)));
            gachaRecordDao.persist(record);
            saved++;
        }
        return saved;
    }

    private LocalDateTime parseTime(String text, LocalDateTime fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(text.trim(), TIME_FORMAT);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /** 模拟抽卡：5 星硬保底 90，4 星 10 抽保底 */
    private List<Reqs.GachaImport.Item> mockItems(int count) {
        Random random = new Random();
        List<Reqs.GachaImport.Item> items = new ArrayList<>();
        int pityFive = 0;
        int pityFour = 0;
        int fiveTarget = 55 + random.nextInt(35);
        int fourTarget = 6 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            pityFive++;
            pityFour++;
            int rarity = 3;
            if (pityFive >= fiveTarget || pityFive >= 90) {
                rarity = 5;
                pityFive = 0;
                fiveTarget = 55 + random.nextInt(35);
            } else if (pityFour >= fourTarget || pityFour >= 10) {
                rarity = 4;
                pityFour = 0;
                fourTarget = 6 + random.nextInt(4);
            } else if (random.nextInt(1000) < 6) {
                rarity = 4;
            }
            Reqs.GachaImport.Item item = new Reqs.GachaImport.Item();
            if (rarity == 5) {
                item.itemName = FIVE_STAR[random.nextInt(FIVE_STAR.length)];
                item.itemType = "角色";
            } else if (rarity == 4) {
                item.itemName = FOUR_STAR[random.nextInt(FOUR_STAR.length)];
                item.itemType = random.nextBoolean() ? "角色" : "武器";
            } else {
                item.itemName = THREE_STAR[random.nextInt(THREE_STAR.length)];
                item.itemType = "武器";
            }
            item.rarity = rarity;
            items.add(item);
        }
        return items;
    }

    public PageResult<Map<String, Object>> page(String poolName, int page, int size) {
        Long userId = currentUser.requireLogin();
        long total = gachaRecordDao.countByUserAndPool(userId, poolName);
        List<GachaRecord> records = gachaRecordDao.listByUser(userId, poolName, (page - 1) * size, size);
        List<Map<String, Object>> items = new ArrayList<>();
        for (GachaRecord record : records) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", record.getId());
            map.put("itemName", record.getItemName());
            map.put("itemType", record.getItemType());
            map.put("rarity", record.getRarity());
            map.put("poolName", record.getPoolName());
            map.put("pullTime", DateUtil.format(record.getPullTime()));
            items.add(map);
        }
        return PageResult.of(total, page, size, items);
    }

    /** 抽卡分析 */
    public Vos.GachaAnalysis analyze(String poolName) {
        Long userId = currentUser.requireLogin();
        List<GachaRecord> records = gachaRecordDao.listAllAsc(userId, poolName);

        Vos.GachaAnalysis vo = new Vos.GachaAnalysis();
        if (records.isEmpty()) {
            return vo;
        }
        int five = 0;
        int four = 0;
        int three = 0;
        for (GachaRecord record : records) {
            int rarity = record.getRarity() == null ? 3 : record.getRarity();
            if (rarity >= 5) {
                five++;
            } else if (rarity == 4) {
                four++;
            } else {
                three++;
            }
        }
        int total = records.size();
        vo.totalPulls = total;
        vo.fiveStar = five;
        vo.fourStar = four;
        vo.threeStar = three;
        vo.fiveStarRate = percent(five, total);
        vo.fourStarRate = percent(four, total);
        vo.avgPullsPerFiveStar = five == 0 ? 0 : Math.round(total * 1.0 / five * 10) / 10.0;

        // 保底统计：记录每次 5 星时的垫抽数
        int pity = 0;
        int maxPity = 0;
        int[] buckets = new int[10];
        for (GachaRecord record : records) {
            pity++;
            if (record.getRarity() != null && record.getRarity() >= 5) {
                maxPity = Math.max(maxPity, pity);
                int bucket = Math.min(9, (pity - 1) / 10);
                buckets[bucket]++;
                pity = 0;
            }
        }
        vo.currentPity = pity;
        vo.maxPity = maxPity;
        for (int i = 0; i < buckets.length; i++) {
            Vos.PityBucket bucket = new Vos.PityBucket();
            bucket.from = i * 10 + 1;
            bucket.to = i == 9 ? 90 : (i + 1) * 10;
            bucket.count = buckets[i];
            vo.pityDistribution.add(bucket);
        }

        // 欧非曲线：累计 5 星率
        int pullIndex = 0;
        int fiveSoFar = 0;
        for (GachaRecord record : records) {
            pullIndex++;
            if (record.getRarity() != null && record.getRarity() >= 5) {
                fiveSoFar++;
                Vos.LuckPoint point = new Vos.LuckPoint();
                point.index = pullIndex;
                point.rate = percent(fiveSoFar, pullIndex);
                vo.luckCurve.add(point);
            }
        }

        // 卡池对比
        Map<String, Vos.PoolStat> poolMap = new LinkedHashMap<>();
        Set<String> pools = new LinkedHashSet<>();
        for (GachaRecord record : records) {
            String name = record.getPoolName() == null ? "未分类" : record.getPoolName();
            pools.add(name);
            Vos.PoolStat stat = poolMap.computeIfAbsent(name, key -> {
                Vos.PoolStat created = new Vos.PoolStat();
                created.poolName = key;
                return created;
            });
            stat.total++;
            if (record.getRarity() != null && record.getRarity() >= 5) {
                stat.fiveStar++;
            } else if (record.getRarity() != null && record.getRarity() == 4) {
                stat.fourStar++;
            }
        }
        for (Vos.PoolStat stat : poolMap.values()) {
            stat.fiveStarRate = percent(stat.fiveStar, stat.total);
            vo.poolStats.add(stat);
        }
        vo.pools = new ArrayList<>(pools);
        return vo;
    }

    @Transactional
    public void clear() {
        Long userId = currentUser.requireLogin();
        gachaRecordDao.deleteByUser(userId);
    }

    private double percent(int part, int total) {
        if (total == 0) {
            return 0;
        }
        return Math.round(part * 10000.0 / total) / 100.0;
    }

    public long countAll() {
        return gachaRecordDao.countAll();
    }

    public Vos.User currentUserInfo() {
        Long userId = currentUser.requireLogin();
        User user = userDao.findRequired(userId);
        return mapper.toUser(user, userId);
    }
}
