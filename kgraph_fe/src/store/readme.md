---
category: Class
subtitle: 运行时数据维护
---

# 运行时数据维护
替代redux

## 何时使用
共享数据需求不高的项目

## API
| 参数 | 说明 | 类型 | 默认值 |
| --- | --- | --- | --- |
| storeProvider | Context Provider Hoc | hoc | 无 |
| dispatch | 被storeProvider、storeConsumer装饰的组件会被挂载props.dispatch，用来设置store值 | function | 无 |
| storeConsumer | Context Consumer Hoc | hoc | 无 |
| store | 被storeConsumer装饰的组件会被挂载props.store，当前store存储的对象在这里 | object | 无 |

### provider
```javascript
@Store.storeProvider()
class Root extends React.PureComponent {
```

### dispatch
```javascript
this.props.dispatch("profile", profile);
```

### consumer
```javascript
@Store.storeConsumer()
class OceanHeader extends React.PureComponent {
```

### store数据读取
```javascript
const roles = this.props.store.roles;
```