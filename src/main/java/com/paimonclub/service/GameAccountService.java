package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.GameAccountDao;
import com.paimonclub.dao.GameCharacterDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.GameAccount;
import com.paimonclub.entity.GameCharacter;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏账号与角色数据服务。
 *
 * <p>所有游戏数据（昵称、冒险等阶、世界等级、角色列表）都由用户自己录入并落库，
 * 系统不生成任何模拟数据。
 */
@ApplicationScoped
public class GameAccountService {

    @Inject GameAccountDao gameAccountDao;
    @Inject GameCharacterDao gameCharacterDao;
    @Inject CurrentUser currentUser;

    @Transactional
    public GameAccount bind(Reqs.BindGame req) {
        Long userId = currentUser.requireLogin();
        String uid = req.gameUid == null ? "" : req.gameUid.trim();
        if (!uid.matches("\\d{9}")) {
            throw BusinessException.badRequest("游戏 UID 必须是 9 位数字");
        }
        GameAccount existing = gameAccountDao.findByUid(uid);
        if (existing != null && !existing.getUserId().equals(userId)) {
            throw BusinessException.badRequest("该 UID 已被其他用户绑定");
        }
        GameAccount account = gameAccountDao.findPrimary(userId);
        if (account == null) {
            account = new GameAccount();
            account.setUserId(userId);
            account.setBindTime(LocalDateTime.now());
        }
        account.setGameUid(uid);
        account.setServer(XssUtil.sanitize(req.server == null || req.server.isBlank() ? "天空岛" : req.server.trim()));
        account.setGameName(XssUtil.sanitize(req.gameName == null || req.gameName.isBlank() ? "原神" : req.gameName.trim()));
        account.setNickname(XssUtil.sanitize(req.nickname == null ? "" : req.nickname.trim()));
        account.setAdventureRank(req.adventureRank);
        account.setWorldLevel(req.worldLevel);
        if (account.getId() == null) {
            gameAccountDao.persist(account);
        } else {
            gameAccountDao.update(account);
        }
        return account;
    }

    public GameAccount myAccount() {
        Long userId = currentUser.requireLogin();
        return gameAccountDao.findPrimary(userId);
    }

    @Transactional
    public void unbind() {
        Long userId = currentUser.requireLogin();
        GameAccount account = gameAccountDao.findPrimary(userId);
        if (account != null) {
            gameAccountDao.remove(account);
        }
        gameCharacterDao.removeByUser(userId);
    }

    /** 查询游戏数据：只返回用户自己录入的真实内容。 */
    public Vos.GameData query(String gameUid) {
        Long userId = currentUser.requireLogin();
        GameAccount account;
        if (gameUid == null || gameUid.isBlank()) {
            account = gameAccountDao.findPrimary(userId);
            if (account == null) {
                throw BusinessException.badRequest("请先绑定游戏 UID");
            }
        } else {
            account = gameAccountDao.findByUid(gameUid);
            if (account == null) {
                throw BusinessException.badRequest("该 UID 尚未绑定");
            }
            if (!account.getUserId().equals(userId)) {
                throw BusinessException.badRequest("只能查看自己绑定的账号");
            }
        }

        Vos.GameData data = new Vos.GameData();
        data.gameUid = account.getGameUid();
        data.server = account.getServer();
        data.nickname = account.getNickname();
        data.adventureRank = account.getAdventureRank();
        data.worldLevel = account.getWorldLevel();
        data.bindTime = DateUtil.format(account.getBindTime());

        Map<String, Integer> elementStats = new LinkedHashMap<>();
        List<GameCharacter> characters = gameCharacterDao.listByUser(userId);
        for (GameCharacter entity : characters) {
            Vos.Character character = new Vos.Character();
            character.name = entity.getName();
            character.element = entity.getElement();
            character.weapon = entity.getWeapon();
            character.level = entity.getLevel();
            character.constellation = entity.getConstellation();
            data.characters.add(character);
            if (entity.getElement() != null && !entity.getElement().isBlank()) {
                elementStats.merge(entity.getElement(), 1, Integer::sum);
            }
        }
        data.elementStats = elementStats;
        return data;
    }

    public List<GameCharacter> listCharacters() {
        Long userId = currentUser.requireLogin();
        return gameCharacterDao.listByUser(userId);
    }

    @Transactional
    public GameCharacter addCharacter(Reqs.GameCharacter req) {
        Long userId = currentUser.requireLogin();
        if (gameAccountDao.findPrimary(userId) == null) {
            throw BusinessException.badRequest("请先绑定游戏 UID");
        }
        GameCharacter entity = new GameCharacter();
        entity.setUserId(userId);
        entity.setName(XssUtil.sanitize(req.name.trim()));
        entity.setElement(XssUtil.sanitize(req.element == null ? "" : req.element.trim()));
        entity.setWeapon(XssUtil.sanitize(req.weapon == null ? "" : req.weapon.trim()));
        entity.setLevel(req.level == null ? 1 : req.level);
        entity.setConstellation(req.constellation == null ? 0 : req.constellation);
        entity.setCreatedAt(LocalDateTime.now());
        gameCharacterDao.persist(entity);
        return entity;
    }

    @Transactional
    public void removeCharacter(Long id) {
        Long userId = currentUser.requireLogin();
        GameCharacter entity = gameCharacterDao.findRequired(id);
        if (!entity.getUserId().equals(userId)) {
            throw BusinessException.badRequest("只能删除自己录入的角色");
        }
        gameCharacterDao.remove(entity);
    }
}
