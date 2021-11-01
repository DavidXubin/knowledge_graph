import React from 'react';
import PropTypes from 'prop-types';
import SystemFrame from '@/component/SystemFrame';
import Store from '@/store';
import authService from '@/service/auth';

@Store.storeConsumer()
@SystemFrame
class PermissionGuard extends React.PureComponent {
  static propTypes = {
    children: PropTypes.element.isRequired,
    store: PropTypes.object.isRequired,
    dispatch: PropTypes.func.isRequired
  };

  async componentDidMount() {
    debugger

    const { store, dispatch } = this.props;

    if (!store.profile) {
      console.log("------------登录-----------");
      const data = await authService.profile();

      if (!data) {
        return;
      }
      dispatch("profile", data);
    }
  }

  render() {
    const { children, store } = this.props;

    if (!store.profile) {
      return null;
    }

    return this.props.children;
  }
}

export default PermissionGuard;
