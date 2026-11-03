/* ============================================================
 * 派蒙社 · 前端公共脚本
 * 统一封装请求、登录态、提示、导航与工具函数
 * ============================================================ */
(function (global) {
    'use strict';

    var TOKEN_KEY = 'pc_token';
    var USER_KEY = 'pc_user';

    var Store = {
        token: function () { return localStorage.getItem(TOKEN_KEY) || ''; },
        setToken: function (t) { localStorage.setItem(TOKEN_KEY, t || ''); },
        user: function () {
            try { return JSON.parse(localStorage.getItem(USER_KEY) || 'null'); } catch (e) { return null; }
        },
        setUser: function (u) { localStorage.setItem(USER_KEY, JSON.stringify(u || null)); },
        clear: function () {
            localStorage.removeItem(TOKEN_KEY);
            localStorage.removeItem(USER_KEY);
        }
    };

    /* 后端枚举 -> 中文，避免页面直接显示 PENDING / COMPLETED 这类裸英文 */
    var LABELS = {
        role: { USER: '普通用户', MODERATOR: '审核员', ADMIN: '管理员' },
        audit: { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' },
        report: { PENDING: '待处理', HANDLED: '已处理', REJECTED: '不成立' },
        order: { PENDING: '待处理', PAID: '已支付', SHIPPED: '已发货', COMPLETED: '已完成', CANCELED: '已取消', CLOSED: '已关闭' },
        productType: { PHYSICAL: '实物', VIRTUAL: '虚拟' },
        decoType: { AVATAR_FRAME: '头像框', TITLE: '头衔' },
        postStatus: { PENDING: '待审核', PUBLISHED: '已发布', REJECTED: '已驳回', TAKEDOWN: '已下架', DRAFT: '草稿' },
        userStatus: { 1: '正常', 0: '已禁用' },
        onOff: { 1: '上架', 0: '下架' },
        pointsType: {
            POST_CREATE: '发帖奖励', LIKE_GIVE: '点赞支出', LIKE_RECEIVE: '被点赞奖励',
            COMMENT: '评论奖励', BE_FAVORITED: '被收藏奖励', SIGN_IN: '每日签到',
            EXCHANGE: '积分兑换', GAME_REWARD: '小游戏奖励', ADMIN_ADJUST: '管理员调整',
            WELCOME: '新人礼包'
        }
    };

    function label(group, value) {
        var m = LABELS[group] || {};
        if (value === null || value === undefined || value === '') { return ''; }
        return m[value] !== undefined ? m[value] : String(value);
    }

    function toast(msg) {
        var el = document.getElementById('pc-toast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'pc-toast';
            el.className = 'toast';
            document.body.appendChild(el);
        }
        el.textContent = msg;
        el.classList.add('show');
        clearTimeout(el._t);
        el._t = setTimeout(function () { el.classList.remove('show'); }, 2400);
    }

    function buildUrl(path, params) {
        var url = path.indexOf('http') === 0 ? path : ('api' + (path.charAt(0) === '/' ? path : '/' + path));
        if (params) {
            var parts = [];
            Object.keys(params).forEach(function (k) {
                var v = params[k];
                if (v === undefined || v === null || v === '') { return; }
                parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v));
            });
            if (parts.length) { url += (url.indexOf('?') >= 0 ? '&' : '?') + parts.join('&'); }
        }
        return url;
    }

    function request(method, path, body, params) {
        var headers = { 'Accept': 'application/json' };
        var token = Store.token();
        if (token) { headers['Authorization'] = 'Bearer ' + token; }
        var options = { method: method, headers: headers };
        if (body !== undefined && body !== null) {
            headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(body);
        }
        return fetch(buildUrl(path, params), options).then(function (resp) {
            return resp.text().then(function (text) {
                var data = null;
                try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
                if (resp.status === 401) {
                    var hadToken = !!token;
                    Store.clear();
                    if (hadToken && location.pathname.indexOf('login') < 0) {
                        toast('登录已过期，请重新登录');
                        setTimeout(function () {
                            location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
                        }, 800);
                    }
                    throw new Error((data && data.msg) || '请先登录');
                }
                if (!resp.ok) {
                    throw new Error((data && data.msg) || ('请求失败（' + resp.status + '）'));
                }
                if (data && typeof data.code !== 'undefined') {
                    if (data.code !== 0 && data.code !== 200) {
                        throw new Error(data.msg || '操作失败');
                    }
                    return data.data;
                }
                return data;
            });
        });
    }

    var api = {
        get: function (p, params) { return request('GET', p, null, params); },
        post: function (p, body, params) { return request('POST', p, body === undefined ? {} : body, params); },
        put: function (p, body) { return request('PUT', p, body === undefined ? {} : body); },
        del: function (p, body) { return request('DELETE', p, body === undefined ? null : body); }
    };

    function login(user, token) {
        Store.setToken(token);
        Store.setUser(user);
    }

    function logout() {
        Store.clear();
        location.href = 'index.html';
    }

    function requireLogin() {
        if (!Store.token()) {
            location.href = 'login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
            return false;
        }
        return true;
    }

    function isStaff() {
        var u = Store.user();
        return !!u && (u.role === 'ADMIN' || u.role === 'MODERATOR');
    }

    function isAdmin() {
        var u = Store.user();
        return !!u && u.role === 'ADMIN';
    }

    var NAV_ITEMS = [
        { href: 'index.html', label: '社区首页', key: 'index' },
        { href: 'gacha-analysis.html', label: '抽卡分析', key: 'gacha' },
        { href: 'game-data.html', label: '游戏数据', key: 'game' },
        { href: 'mall.html', label: '积分商城', key: 'mall' },
        { href: 'social.html', label: '好友私信', key: 'social' },
        { href: 'games.html', label: '小游戏', key: 'games' },
        { href: 'user-center.html', label: '个人中心', key: 'user' }
    ];

    var navActive = '';

    function renderNav(active) {
        var host = document.getElementById('pc-nav');
        if (!host) { return; }
        navActive = active || '';
        var user = Store.user();
        var links = NAV_ITEMS.map(function (item) {
            var cls = item.key === active ? 'active' : '';
            return '<a class="' + cls + '" href="' + item.href + '">' + item.label + '</a>';
        }).join('');
        if (isStaff()) {
            links += '<a class="' + (active === 'admin' ? 'active' : '') + '" href="admin.html">后台管理</a>';
        }
        var right;
        if (user) {
            right = '<a href="post-create.html" class="btn btn-sm">发帖</a>' +
                '<a href="user-center.html" title="' + escapeHtml(user.nickname || user.username) + '">' + avatarBox(user, 'sm') + '</a>' +
                '<span style="font-size:13px;color:#4c5666">' + escapeHtml(user.nickname || user.username) + '</span>' +
                '<a href="javascript:void(0)" id="pc-logout" style="font-size:13px">退出</a>';
        } else {
            right = '<a href="post-create.html" class="btn btn-sm">发帖</a>' +
                '<a href="login.html" class="btn btn-sm btn-ghost">登录</a>' +
                '<a href="register.html" class="btn btn-sm">注册</a>';
        }
        host.innerHTML =
            '<div class="nav"><div class="nav-inner">' +
            '<a class="brand" href="index.html"><span class="dot"></span>派蒙社</a>' +
            '<div class="nav-links">' + links + '</div>' +
            '<div class="nav-user">' + right + '</div>' +
            '</div></div>';
        var out = document.getElementById('pc-logout');
        if (out) { out.addEventListener('click', logout); }
        if (user) { refreshUnread(); }
    }

    /** 拉一次未读私信数，给导航栏「好友私信」加红点 */
    function refreshUnread() {
        api.get('users/notifications/unread').then(function (n) {
            var count = Number(n) || 0;
            var link = document.querySelector('.nav-links a[href="social.html"]');
            if (!link) { return; }
            var old = link.querySelector('.nav-dot');
            if (old) { old.remove(); }
            if (count > 0) {
                var dot = document.createElement('span');
                dot.className = 'nav-dot';
                dot.textContent = count > 99 ? '99+' : String(count);
                link.appendChild(dot);
            }
        }).catch(function () { /* 未登录或接口异常时忽略 */ });
    }

    function escapeHtml(text) {
        if (text === null || text === undefined) { return ''; }
        return String(text)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function qs(name) {
        var m = new RegExp('[?&]' + name + '=([^&]*)').exec(location.search);
        return m ? decodeURIComponent(m[1].replace(/\+/g, ' ')) : '';
    }

    function param(name, fallback) {
        var v = qs(name);
        return v === '' ? (fallback === undefined ? '' : fallback) : v;
    }

    /**
     * 资源路径归一化。
     * 数据库里既有 "static/img/x.svg" 也有 "/static/img/x.svg" 两种历史写法，
     * 后者在子路径部署（如 IDEA 的 /PaimonClub_war_exploded/）时会 404，
     * 统一转成相对路径，两种部署方式都能正确加载。
     */
    function asset(url) {
        if (!url) { return ''; }
        if (url.indexOf('/static/') === 0) { return url.substring(1); }
        return url;
    }

    function avatar(url) {
        return asset(url) || 'static/img/avatar-default.svg';
    }

    /**
     * 带装扮的头像：头像 + 佩戴中的头像框叠加。
     * user 需要包含 avatarUrl / wearingFrame（后端 Vos.User 已返回）。
     */
    function avatarBox(user, size) {
        var u = user || {};
        var frame = u.wearingFrame
            ? '<img class="avatar-frame" src="' + asset(u.wearingFrame) + '" alt="">'
            : '';
        return '<span class="avatar-wrap' + (size === 'sm' ? ' sm' : '') + '">' +
            '<img class="avatar-img" src="' + avatar(u.avatarUrl) + '" alt="">' + frame + '</span>';
    }

    function empty(text) {
        return '<div class="empty">' + escapeHtml(text || '暂无数据') + '</div>';
    }

    /** 未登录占位：给出「去登录」入口，而不是让页面空白或报错 */
    function guestHint(container, text) {
        var host = typeof container === 'string' ? document.getElementById(container) : container;
        if (!host) { return; }
        var back = encodeURIComponent(location.pathname + location.search);
        host.innerHTML = '<div class="empty">' + escapeHtml(text || '登录后即可使用该功能') +
            '<div style="margin-top:12px"><a class="btn btn-sm" href="login.html?redirect=' + back + '">去登录</a></div></div>';
    }

    function pager(container, page, pages, onChange) {
        var host = typeof container === 'string' ? document.getElementById(container) : container;
        if (!host) { return; }
        if (!pages || pages <= 1) { host.innerHTML = ''; return; }
        var html = '<div class="pager">';
        html += '<button class="btn btn-sm btn-line" data-p="' + Math.max(1, page - 1) + '"' + (page <= 1 ? ' disabled' : '') + '>上一页</button>';
        html += '<span style="padding:6px 10px;color:#7c8798;font-size:13px">' + page + ' / ' + pages + '</span>';
        html += '<button class="btn btn-sm btn-line" data-p="' + Math.min(pages, page + 1) + '"' + (page >= pages ? ' disabled' : '') + '>下一页</button>';
        html += '</div>';
        host.innerHTML = html;
        host.querySelectorAll('button[data-p]').forEach(function (btn) {
            btn.addEventListener('click', function () {
                var p = parseInt(btn.getAttribute('data-p'), 10);
                if (p && p !== page) { onChange(p); }
            });
        });
    }

    function modal(title, bodyHtml, onOk, okText) {
        var mask = document.createElement('div');
        mask.className = 'modal-mask show';
        mask.innerHTML = '<div class="modal"><div class="card-title">' + escapeHtml(title) + '</div>' +
            '<div class="modal-body">' + bodyHtml + '</div>' +
            '<div style="display:flex;gap:10px;justify-content:flex-end;margin-top:16px">' +
            '<button class="btn btn-line" data-close>取消</button>' +
            '<button class="btn" data-ok>' + escapeHtml(okText || '确定') + '</button></div></div>';
        document.body.appendChild(mask);
        function close() { mask.remove(); }
        mask.querySelector('[data-close]').addEventListener('click', close);
        mask.addEventListener('click', function (e) { if (e.target === mask) { close(); } });
        mask.querySelector('[data-ok]').addEventListener('click', function () {
            var result = onOk ? onOk(mask) : undefined;
            if (result === false) { return; }
            close();
        });
        return { el: mask, close: close };
    }

    function fmtTime(value) {
        if (!value) { return ''; }
        return String(value).replace('T', ' ').slice(0, 16);
    }

    /** 简易 SVG 折线图：data = [{label, value}] */
    function lineChart(host, data, color) {
        var el = typeof host === 'string' ? document.getElementById(host) : host;
        if (!el) { return; }
        if (!data || !data.length) { el.innerHTML = empty('暂无数据'); return; }
        var w = el.clientWidth || 520, h = 220, pad = 34;
        var max = Math.max.apply(null, data.map(function (d) { return d.value; }).concat([1]));
        var stepX = data.length > 1 ? (w - pad * 2) / (data.length - 1) : 0;
        var pts = data.map(function (d, i) {
            var x = pad + i * stepX;
            var y = h - pad - (d.value / max) * (h - pad * 2);
            return [x, y];
        });
        var path = pts.map(function (p, i) { return (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1); }).join(' ');
        var area = path + ' L' + pts[pts.length - 1][0].toFixed(1) + ' ' + (h - pad) + ' L' + pts[0][0].toFixed(1) + ' ' + (h - pad) + ' Z';
        var labels = data.map(function (d, i) {
            if (data.length > 8 && i % Math.ceil(data.length / 8) !== 0 && i !== data.length - 1) { return ''; }
            return '<text x="' + pts[i][0].toFixed(1) + '" y="' + (h - pad + 16) + '" font-size="10" fill="#7c8798" text-anchor="middle">' + escapeHtml(d.label) + '</text>';
        }).join('');
        el.innerHTML = '<svg viewBox="0 0 ' + w + ' ' + h + '" width="100%" height="100%">' +
            '<line x1="' + pad + '" y1="' + (h - pad) + '" x2="' + (w - pad) + '" y2="' + (h - pad) + '" stroke="#e6eaf2"/>' +
            '<path d="' + area + '" fill="' + (color || '#4a6cf7') + '" opacity="0.12"/>' +
            '<path d="' + path + '" fill="none" stroke="' + (color || '#4a6cf7') + '" stroke-width="2"/>' +
            pts.map(function (p) { return '<circle cx="' + p[0].toFixed(1) + '" cy="' + p[1].toFixed(1) + '" r="2.6" fill="' + (color || '#4a6cf7') + '"/>'; }).join('') +
            labels + '</svg>';
    }

    /** 简易柱状图：data = [{label, value}] */
    function barChart(host, data, color) {
        var el = typeof host === 'string' ? document.getElementById(host) : host;
        if (!el) { return; }
        if (!data || !data.length) { el.innerHTML = empty('暂无数据'); return; }
        var w = el.clientWidth || 520, h = 220, pad = 30;
        var max = Math.max.apply(null, data.map(function (d) { return d.value; }).concat([1]));
        var n = data.length;
        var bw = Math.min(46, (w - pad * 2) / n * 0.62);
        var gap = (w - pad * 2) / n;
        var bars = data.map(function (d, i) {
            var bh = (d.value / max) * (h - pad * 2);
            var x = pad + i * gap + (gap - bw) / 2;
            var y = h - pad - bh;
            return '<rect x="' + x.toFixed(1) + '" y="' + y.toFixed(1) + '" width="' + bw.toFixed(1) + '" height="' + bh.toFixed(1) + '" rx="5" fill="' + (color || '#4a6cf7') + '"/>' +
                '<text x="' + (x + bw / 2).toFixed(1) + '" y="' + (y - 6).toFixed(1) + '" font-size="10" fill="#7c8798" text-anchor="middle">' + d.value + '</text>' +
                '<text x="' + (x + bw / 2).toFixed(1) + '" y="' + (h - pad + 16) + '" font-size="10" fill="#7c8798" text-anchor="middle">' + escapeHtml(d.label) + '</text>';
        }).join('');
        el.innerHTML = '<svg viewBox="0 0 ' + w + ' ' + h + '" width="100%" height="100%">' +
            '<line x1="' + pad + '" y1="' + (h - pad) + '" x2="' + (w - pad) + '" y2="' + (h - pad) + '" stroke="#e6eaf2"/>' +
            bars + '</svg>';
    }

    /**
     * 重新拉取当前用户资料并刷新导航。
     * localStorage 里的 user 是登录瞬间的快照，佩戴装扮、改昵称之后不会自动更新，
     * 调用这里可以让头像框、头衔、积分立刻反映到全站导航。
     */
    function refreshMe() {
        if (!Store.token()) { return Promise.resolve(null); }
        return api.get('users/me').then(function (u) {
            if (u) {
                Store.setUser(u);
                renderNav(navActive);
            }
            return u;
        }).catch(function () { return null; });
    }

    global.PC = {
        api: api, Store: Store, toast: toast, login: login, logout: logout,
        requireLogin: requireLogin, isStaff: isStaff, isAdmin: isAdmin,
        renderNav: renderNav, escapeHtml: escapeHtml, avatar: avatar,
        asset: asset, avatarBox: avatarBox,
        label: label, refreshMe: refreshMe, refreshUnread: refreshUnread,
        param: param, qs: qs, empty: empty, pager: pager, modal: modal,
        guestHint: guestHint,
        fmtTime: fmtTime, lineChart: lineChart, barChart: barChart
    };
})(window);
