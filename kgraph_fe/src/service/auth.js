import { remote } from '@/untils/fetch';

class Auth {
   profile(params) {
    return remote('/server/auth/profile', 'POST', params);
  }

   logout(params) {
    return remote('/server/auth/logout', 'POST', params);
  }
}

export default new Auth();
