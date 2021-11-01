import {fetch} from 'ocean-utils';
import {message, Modal} from 'antd';

const confirm = Modal.confirm;

let defaultHeader = {};

export const setDefaultHeader = header => {
    defaultHeader = header;
};

const handleResponse = response => {
    console.log(response);
    if (response.code !== 200) {
        return Promise.reject(response);
    }
    return Promise.resolve(response.result);
};

const handleError = response => {
    const {code, result, msg} = response;

    switch (code) {
        case 403:
            confirm({
                title: '提示',
                content: '登录信息已失效，请重新登录.',
                okText: '去登录',
                cancelText: '取消',
                onOk() {
                    const {url} = result;
                    window.location.href = url;
                }
            });
            break;
        case 501:
            confirm({
                title: '提示',
                content: '没有访问该关系图谱的权限',
                // okText: '去登录',
                cancelText: '取消',
            });
            break;

        default:
            message.error(msg || '未知错误');
            break;
    }
};

const remote = (url, method = 'POST', params = {}, silent = false) => {

    let header = {};
    let data = {data: params};
    if (typeof url === 'object') {
        header = url.header;
        url = url.url;
    }

    if (method.toUpperCase() === 'GET') {
        data = {
            params
        };
    }

    return fetch
        .axios({
            method,
            url,
            ...data,
            headers: {...defaultHeader, ...header}
        })
        .then(result => {
            return handleResponse(result);
        })
        .catch(err => {
            if (!silent) handleError(err);
            else console.log(err);
        });
};

export {remote};
