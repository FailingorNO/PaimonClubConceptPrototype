package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.GameScoreDao;
import com.paimonclub.dao.MiniGameDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.GameScore;
import com.paimonclub.entity.MiniGame;
import com.paimonclub.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小游戏服务：游戏列表、成绩提交、积分奖励、排行榜。
 */
@ApplicationScoped
public class MiniGameService {

    @Inject MiniGameDao miniGameDao;
    @Inject GameScoreDao gameScoreDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;
    @Inject com.paimonclub.dao.PointsRecordDao pointsRecordDao;

    /** 单局成绩上限，超过基本可以判定为改包或改前端 */
    private static final int MAX_SCORE = 1_000_000;
    /** 同一用户同一游戏的最短提交间隔（毫秒） */
    private static final long MIN_SUBMIT_INTERVAL_MS = 3_000;
    /** 每个游戏每天最多领取几次新纪录奖励 */
    private static final int MAX_DAILY_GAME_REWARD = 5;

    private final java.util.Map<String, Long> lastSubmitAt = new java.util.concurrent.ConcurrentHashMap<>();

    public List<Vos.MiniGame> list() {
        Long userId = currentUser.getUserId();
        List<Vos.MiniGame> items = new ArrayList<>();
        for (MiniGame game : miniGameDao.listEnabled()) {
            Vos.MiniGame vo = new Vos.MiniGame();
            vo.id = game.getId();
            vo.name = game.getName();
            vo.path = game.getPath();
            vo.rewardPoints = game.getRewardPoints();
            vo.description = game.getDescription();
            vo.icon = game.getIcon();
            vo.gameKey = game.getGameKey();
            if (userId != null) {
                vo.bestScore = gameScoreDao.bestOf(userId, game.getId());
            }
            items.add(vo);
        }
        return items;
    }

    @Transactional
    public Map<String, Object> submitScore(Reqs.Score req) {
        Long userId = currentUser.requireLogin();
        MiniGame game = miniGameDao.findRequired(req.gameId);
        if (game.getStatus() == null || game.getStatus() != 1) {
            throw BusinessException.badRequest("游戏已下架");
        }
        if (req.score == null || req.score < 0) {
            throw BusinessException.badRequest("成绩不合法");
        }
        // 服务端兜底：分数上限、提交频率、单局耗时，防止直接调接口刷分
        if (req.score > MAX_SCORE) {
            throw BusinessException.badRequest("成绩超出合理范围");
        }
        String submitKey = userId + ":" + game.getId();
        long nowMillis = System.currentTimeMillis();
        Long lastAt = lastSubmitAt.get(submitKey);
        if (lastAt != null && nowMillis - lastAt < MIN_SUBMIT_INTERVAL_MS) {
            throw BusinessException.badRequest("提交太频繁，请稍后再试");
        }
        if (req.durationMs != null && req.durationMs >= 0 && req.durationMs < 300 && req.score > 0) {
            throw BusinessException.badRequest("成绩提交过快，疑似异常数据");
        }
        // 全部校验通过后才计时：被拒的请求不能把正常玩家挡在门外
        lastSubmitAt.put(submitKey, nowMillis);
        Integer best = gameScoreDao.bestOf(userId, game.getId());
        GameScore score = new GameScore();
        score.setUserId(userId);
        score.setGameId(game.getId());
        score.setScore(req.score);
        score.setCreatedAt(LocalDateTime.now());
        gameScoreDao.persist(score);

        boolean newRecord = best == null || req.score > best;
        int reward = 0;
        boolean rewardable = newRecord
                && pointsRecordDao.countToday(userId, Constants.POINT_TYPE_GAME_REWARD) < MAX_DAILY_GAME_REWARD;
        if (rewardable && game.getRewardPoints() != null && game.getRewardPoints() > 0) {
            reward = pointsService.award(userId, game.getRewardPoints(), Constants.POINT_TYPE_GAME_REWARD,
                    game.getId(), "小游戏《" + game.getName() + "》新纪录");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("newRecord", newRecord);
        result.put("bestScore", Math.max(best == null ? 0 : best, req.score));
        result.put("reward", reward);
        return result;
    }

    public List<Vos.Score> leaderboard(Long gameId, int limit) {
        List<Vos.Score> items = new ArrayList<>();
        int rank = 1;
        for (Object[] row : gameScoreDao.topByGame(gameId, limit)) {
            Vos.Score vo = new Vos.Score();
            vo.rank = rank++;
            vo.userId = ((Number) row[0]).longValue();
            vo.score = ((Number) row[1]).intValue();
            User user = userDao.find(vo.userId);
            vo.userName = user == null ? "未知用户" : user.getNickname();
            vo.avatarUrl = user == null ? Constants.DEFAULT_AVATAR : user.getAvatarUrl();
            items.add(vo);
        }
        return items;
    }
}
