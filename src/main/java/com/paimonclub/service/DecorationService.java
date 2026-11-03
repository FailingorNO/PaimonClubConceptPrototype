package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.DecorationDao;
import com.paimonclub.dao.UserDecorationDao;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Decoration;
import com.paimonclub.entity.UserDecoration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 装扮服务：头像框、头衔、主题的购买与佩戴。
 */
@ApplicationScoped
public class DecorationService {

    @Inject DecorationDao decorationDao;
    @Inject UserDecorationDao userDecorationDao;
    @Inject CurrentUser currentUser;
    @Inject PointsService pointsService;

    /** 装扮商城列表（含拥有/佩戴状态） */
    public List<Vos.Decoration> store() {
        Long userId = currentUser.getUserId();
        Set<Long> owned = new HashSet<>();
        Set<Long> wearing = new HashSet<>();
        if (userId != null) {
            for (UserDecoration item : userDecorationDao.listByUser(userId)) {
                owned.add(item.getDecorationId());
                if (Boolean.TRUE.equals(item.getIsWearing())) {
                    wearing.add(item.getDecorationId());
                }
            }
        }
        List<Vos.Decoration> items = new ArrayList<>();
        for (Decoration decoration : decorationDao.listByStatus(1)) {
            Vos.Decoration vo = new Vos.Decoration();
            vo.id = decoration.getId();
            vo.name = decoration.getName();
            vo.type = decoration.getType();
            vo.resourceUrl = decoration.getResourceUrl();
            vo.price = decoration.getPrice();
            vo.owned = owned.contains(decoration.getId());
            vo.wearing = wearing.contains(decoration.getId());
            items.add(vo);
        }
        return items;
    }

    /** 我的装扮 */
    public List<Vos.Decoration> mine() {
        Long userId = currentUser.requireLogin();
        List<Vos.Decoration> items = new ArrayList<>();
        for (UserDecoration item : userDecorationDao.listByUser(userId)) {
            Decoration decoration = decorationDao.find(item.getDecorationId());
            if (decoration == null) {
                continue;
            }
            Vos.Decoration vo = new Vos.Decoration();
            vo.id = decoration.getId();
            vo.name = decoration.getName();
            vo.type = decoration.getType();
            vo.resourceUrl = decoration.getResourceUrl();
            vo.price = decoration.getPrice();
            vo.owned = true;
            vo.wearing = Boolean.TRUE.equals(item.getIsWearing());
            items.add(vo);
        }
        return items;
    }

    @Transactional
    public Vos.Decoration buy(Long decorationId) {
        Long userId = currentUser.requireLogin();
        Decoration decoration = decorationDao.findRequired(decorationId);
        if (decoration.getStatus() == null || decoration.getStatus() != 1) {
            throw BusinessException.badRequest("该装扮已下架");
        }
        if (userDecorationDao.exists(userId, decorationId)) {
            throw BusinessException.badRequest("你已经拥有该装扮");
        }
        pointsService.spend(userId, decoration.getPrice(), Constants.POINT_TYPE_EXCHANGE,
                decorationId, "购买装扮：" + decoration.getName());
        UserDecoration item = new UserDecoration();
        item.setUserId(userId);
        item.setDecorationId(decorationId);
        item.setIsWearing(false);
        item.setExpireAt(LocalDateTime.now().plusDays(30));
        item.setCreatedAt(LocalDateTime.now());
        userDecorationDao.persist(item);

        Vos.Decoration vo = new Vos.Decoration();
        vo.id = decoration.getId();
        vo.name = decoration.getName();
        vo.type = decoration.getType();
        vo.resourceUrl = decoration.getResourceUrl();
        vo.price = decoration.getPrice();
        vo.owned = true;
        vo.wearing = false;
        return vo;
    }

    /** 佩戴/取消佩戴（同类型只能佩戴一个） */
    @Transactional
    public void wear(Long decorationId, boolean wearing) {
        Long userId = currentUser.requireLogin();
        Decoration decoration = decorationDao.findRequired(decorationId);
        UserDecoration target = null;
        for (UserDecoration item : userDecorationDao.listByUser(userId)) {
            if (item.getDecorationId().equals(decorationId)) {
                target = item;
                break;
            }
        }
        if (target == null) {
            throw BusinessException.badRequest("你还没有购买该装扮");
        }
        if (wearing) {
            for (UserDecoration item : userDecorationDao.listByUser(userId)) {
                Decoration other = decorationDao.find(item.getDecorationId());
                if (other != null && decoration.getType().equals(other.getType())
                        && Boolean.TRUE.equals(item.getIsWearing())) {
                    item.setIsWearing(false);
                    userDecorationDao.update(item);
                }
            }
        }
        target.setIsWearing(wearing);
        userDecorationDao.update(target);
    }
}
