import {remote} from '@/untils/fetch';

class PermissionService {
    checkPermission(params) {
        return remote('/server/permission/check', 'POST', params);
    }
}

export default new PermissionService();