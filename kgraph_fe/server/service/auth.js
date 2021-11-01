const {request} = require('bk-fe-fetch');
const config = require('../config');
const LRU = require('../utils/lru');
const {getSSOUrl} = require('../utils/tool');

// 查询该用户所有角色在指定项目列表中的权限
const getAppPermissionByRoleId = async (ctx, roleIds = []) => {
    console.log('==================== role permission ====================');
    console.log(config.uc.authority);
    const res = await request(ctx)(config.uc.authority, {
        method: 'POST',
        body: JSON.stringify({
            roleIds,
            applicationCodes: config.appIds
        })
    });
    return res.data
};

const profile = async ctx => {
    const secret = ctx.cookies.get(config.uc.secret);
    if (secret) {
        const res = await request(ctx)(`${config.uc.passport}`);
        console.log('==================== userInfo ====================');
        const userInfo = res.data;
        console.log(userInfo)
        if (userInfo.user && userInfo.user.code) {
            const roleIds = await getRoleList(ctx, userInfo.user.code);
            const permissionList = await getAppPermissionByRoleId(ctx, roleIds);
            return {
                code: 200,
                result: {
                    roles: permissionList,
                    ...userInfo.user
                }
            }
        }
        return {code: 403, message: '请登录', result: {url: getSSOUrl(ctx)}};
    }
    return {code: 403, message: '请登录', result: {url: getSSOUrl(ctx)}};

};

// 通过用户usercode获取所有角色
const getRoleList = async (ctx, userCode) => {
    const url = `${config.uc.uc}/${userCode}/roles`;
    console.log('==================== get roles ====================');
    console.log(url);
    const roleList = await request(ctx)(url);
    return (roleList && roleList.data || []).map(item => item.id);
}

const logout = async ctx => {
    const secret = ctx.cookies.get(config.uc.secret);

    if (secret) {
        const res = await request(ctx)(`${config.uc.passport}`, {
            method: 'DELETE',
        });

        LRU.del(secret);

        return {
            code: 200,
            result: {
                value: res,
                url: getSSOUrl(ctx)
            }
        };
    }
};

module.exports = {
    profile,
    logout
};
