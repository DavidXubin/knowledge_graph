/**
 * 使用context api代替redux，适合共享数据需求不高的项目
 */

import React from 'react';

class Store {
  constructor() {
    this.context = React.createContext('store');
  }

  storeProvider() {
    const Context = this.context;
    return WrappedComponent =>
      class component extends React.PureComponent {
        state = {
          value: {}
        };

        dispatch = (key, value) => {
          this.setState(prevState => {
            const nextStore = JSON.parse(JSON.stringify(prevState.value));
            nextStore[key] = value;
            return {
              value: nextStore
            };
          });
        };

        render() {
          return (
            <Context.Provider
              value={{
                data: this.state.value,
                dispatch: this.dispatch
              }}
            >
              <WrappedComponent {...this.props} dispatch={this.dispatch} />
            </Context.Provider>
          );
        }
      };
  }

  storeConsumer() {
    const Context = this.context;
    return WrappedComponent =>
      function component(props) {
        return <Context.Consumer>{store => <WrappedComponent {...props} store={store.data} dispatch={store.dispatch} />}</Context.Consumer>;
      };
  }
}

const instance = new Store();

export default instance;
