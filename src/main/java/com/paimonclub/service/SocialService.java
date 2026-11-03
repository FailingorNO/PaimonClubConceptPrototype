package com.paimonclub.service;

import com.paimonclub.common.BusinessException;
import com.paimonclub.common.Constants;
import com.paimonclub.common.CurrentUser;
import com.paimonclub.dao.FriendDao;
import com.paimonclub.dao.FollowDao;
import com.paimonclub.dao.MessageDao;
import com.paimonclub.dao.UserDao;
import com.paimonclub.dto.request.Reqs;
import com.paimonclub.dto.response.Vos;
import com.paimonclub.entity.Follow;
import com.paimonclub.entity.Friend;
import com.paimonclub.entity.Message;
import com.paimonclub.entity.User;
import com.paimonclub.util.DateUtil;
import com.paimonclub.util.XssUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 社交服务：关注、粉丝、好友、私信、黑名单。
 */
@ApplicationScoped
public class SocialService {

    @Inject FollowDao followDao;
    @Inject FriendDao friendDao;
    @Inject MessageDao messageDao;
    @Inject UserDao userDao;
    @Inject CurrentUser currentUser;
    @Inject VoMapper mapper;
    @Inject NotificationService notificationService;

    /** 关注/取消关注 */
    @Transactional
    public boolean toggleFollow(Long targetUserId) {
        Long userId = currentUser.requireLogin();
        if (userId.equals(targetUserId)) {
            throw BusinessException.badRequest("不能关注自己");
        }
        User target = userDao.findRequired(targetUserId);
        if (followDao.exists(userId, targetUserId)) {
            followDao.deleteOne(userId, targetUserId);
            return false;
        }
        Follow follow = new Follow();
        follow.setFollowerId(userId);
        follow.setFolloweeId(targetUserId);
        follow.setCreatedAt(LocalDateTime.now());
        followDao.persist(follow);
        User actor = userDao.find(userId);
        notificationService.push(targetUserId, Constants.NOTIFY_FOLLOW,
                (actor == null ? "有人" : actor.getNickname()) + " 关注了你", userId);
        return true;
    }

    public List<Vos.User> followers(Long userId) {
        List<Vos.User> items = new ArrayList<>();
        for (Long id : followDao.followerIds(userId)) {
            User user = userDao.find(id);
            if (user != null) {
                items.add(mapper.toUser(user, currentUser.getUserId()));
            }
        }
        return items;
    }

    public List<Vos.User> followees(Long userId) {
        List<Vos.User> items = new ArrayList<>();
        for (Long id : followDao.followeeIds(userId)) {
            User user = userDao.find(id);
            if (user != null) {
                items.add(mapper.toUser(user, currentUser.getUserId()));
            }
        }
        return items;
    }

    /** 好友申请/同意/拒绝/删除/拉黑 */
    @Transactional
    public String friendAction(Reqs.FriendAction req) {
        Long userId = currentUser.requireLogin();
        Long friendId = req.friendId;
        if (userId.equals(friendId)) {
            throw BusinessException.badRequest("不能对自己操作");
        }
        userDao.findRequired(friendId);
        String action = req.action == null ? "" : req.action.toUpperCase();
        Friend relation = friendDao.findBetween(userId, friendId);

        switch (action) {
            case "APPLY" -> {
                if (relation != null) {
                    if (Constants.FRIEND_ACCEPTED.equals(relation.getStatus())) {
                        throw BusinessException.badRequest("你们已经是好友了");
                    }
                    if (Constants.FRIEND_BLOCKED.equals(relation.getStatus())) {
                        if (friendId.equals(relation.getUserId())) {
                            throw BusinessException.forbidden("对方已将你拉黑");
                        }
                        throw BusinessException.badRequest("你已把对方拉黑，请先解除黑名单");
                    }
                    throw BusinessException.badRequest("已发送过好友申请，等待对方处理");
                }
                Friend friend = new Friend();
                friend.setUserId(userId);
                friend.setFriendId(friendId);
                friend.setStatus(Constants.FRIEND_PENDING);
                friend.setCreatedAt(LocalDateTime.now());
                friend.setUpdatedAt(LocalDateTime.now());
                friendDao.persist(friend);
                User actor = userDao.find(userId);
                notificationService.push(friendId, Constants.NOTIFY_SYSTEM,
                        (actor == null ? "有人" : actor.getNickname()) + " 申请加你为好友", userId);
                return "好友申请已发送";
            }
            case "ACCEPT" -> {
                if (relation == null || !Constants.FRIEND_PENDING.equals(relation.getStatus())) {
                    throw BusinessException.badRequest("没有待处理的好友申请");
                }
                if (!friendId.equals(relation.getUserId())) {
                    throw BusinessException.forbidden("只能由被申请方同意");
                }
                relation.setStatus(Constants.FRIEND_ACCEPTED);
                relation.setUpdatedAt(LocalDateTime.now());
                friendDao.update(relation);
                notificationService.push(friendId, Constants.NOTIFY_SYSTEM, "你们已经成为好友，快去私信吧", userId);
                return "已同意好友申请";
            }
            case "REJECT" -> {
                if (relation == null || !Constants.FRIEND_PENDING.equals(relation.getStatus())) {
                    throw BusinessException.badRequest("没有待处理的好友申请");
                }
                // 双向一起清，避免历史数据里另一方向还有一条残留
                friendDao.deleteBetween(userId, friendId);
                return "已拒绝好友申请";
            }
            case "DELETE" -> {
                if (relation == null) {
                    throw BusinessException.badRequest("你们还不是好友");
                }
                friendDao.deleteBetween(userId, friendId);
                return "已解除好友关系";
            }
            case "BLOCK" -> {
                // 拉黑必须方向明确：先清掉两个方向的旧记录，再写入「我 → 对方」这一条
                friendDao.deleteBetween(userId, friendId);
                Friend blocked = new Friend();
                blocked.setUserId(userId);
                blocked.setFriendId(friendId);
                blocked.setStatus(Constants.FRIEND_BLOCKED);
                blocked.setCreatedAt(LocalDateTime.now());
                blocked.setUpdatedAt(LocalDateTime.now());
                friendDao.persist(blocked);
                return "已加入黑名单";
            }
            case "UNBLOCK" -> {
                // 只解除我主动拉黑的方向；对方拉黑我的那条要由对方自己解除
                Friend mine = friendDao.findBetween(userId, friendId);
                if (mine != null && Constants.FRIEND_BLOCKED.equals(mine.getStatus()) && userId.equals(mine.getUserId())) {
                    friendDao.remove(mine);
                }
                return "已解除黑名单";
            }
            default -> throw BusinessException.badRequest("不支持的操作：" + action);
        }
    }

    public List<Vos.RelationItem> friends() {
        Long userId = currentUser.requireLogin();
        return toRelationItems(userId, friendDao.listByStatus(userId, Constants.FRIEND_ACCEPTED));
    }

    public List<Vos.RelationItem> friendRequests() {
        Long userId = currentUser.requireLogin();
        return toRelationItems(userId, friendDao.listIncoming(userId, Constants.FRIEND_PENDING));
    }

    public List<Vos.RelationItem> blocked() {
        Long userId = currentUser.requireLogin();
        // 只看我拉黑的人，不把「拉黑我的人」混进来
        return toRelationItems(userId, friendDao.listMyBlocked(userId));
    }

    private List<Vos.RelationItem> toRelationItems(Long userId, List<Friend> relations) {
        List<Vos.RelationItem> items = new ArrayList<>();
        for (Friend relation : relations) {
            Long otherId = relation.getUserId().equals(userId) ? relation.getFriendId() : relation.getUserId();
            User other = userDao.find(otherId);
            if (other == null) {
                continue;
            }
            Vos.RelationItem item = new Vos.RelationItem();
            item.user = mapper.toUser(other, userId);
            item.status = relation.getStatus();
            item.relationId = relation.getId();
            item.incoming = relation.getFriendId().equals(userId);
            item.createdAt = DateUtil.format(relation.getCreatedAt());
            items.add(item);
        }
        return items;
    }

    @Transactional
    public Vos.Message sendMessage(Reqs.Message req) {
        Long userId = currentUser.requireLogin();
        userDao.findRequired(req.toUser);
        if (userId.equals(req.toUser)) {
            throw BusinessException.badRequest("不能给自己发私信");
        }
        // 任一方向拉黑都禁止投递，否则黑名单形同虚设
        if (friendDao.isBlocked(userId, req.toUser)) {
            throw BusinessException.forbidden("你和对方之间存在黑名单关系，无法发送私信");
        }
        Message message = new Message();
        message.setFromUser(userId);
        message.setToUser(req.toUser);
        message.setContent(XssUtil.sanitize(req.content.trim()));
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());
        messageDao.persist(message);
        return toMessageVo(message);
    }

    public List<Vos.Message> conversation(Long otherUserId) {
        Long userId = currentUser.requireLogin();
        // 先记下进入会话的时刻：只把此刻之前到达的消息标记为已读
        LocalDateTime enteredAt = LocalDateTime.now();
        List<Vos.Message> items = new ArrayList<>();
        for (Message message : messageDao.listConversation(userId, otherUserId, 100)) {
            items.add(toMessageVo(message));
        }
        messageDao.markRead(otherUserId, userId, enteredAt);
        return items;
    }

    public List<Vos.Message> inbox() {
        Long userId = currentUser.requireLogin();
        List<Vos.Message> items = new ArrayList<>();
        for (Message message : messageDao.listInbox(userId, 50)) {
            items.add(toMessageVo(message));
        }
        return items;
    }

    private Vos.Message toMessageVo(Message message) {
        Vos.Message vo = new Vos.Message();
        vo.id = message.getId();
        vo.fromUser = message.getFromUser();
        vo.toUser = message.getToUser();
        User from = userDao.find(message.getFromUser());
        vo.fromName = from == null ? "未知用户" : from.getNickname();
        vo.fromAvatar = from == null ? Constants.DEFAULT_AVATAR : from.getAvatarUrl();
        vo.content = message.getContent();
        vo.isRead = message.getIsRead();
        vo.createdAt = DateUtil.format(message.getCreatedAt());
        return vo;
    }
}
