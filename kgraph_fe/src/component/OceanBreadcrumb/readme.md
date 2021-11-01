---
category: HOC
subtitle: 面包屑通讯装饰器
---

# 面包屑
装饰器模式HOC，动态为组件添加与breadcrumb通讯的能力。配合SystemFrame一起使用

## 效果图
![效果图](https://storage.bkjk.com/storage/file/5a2532c2638366022aa719f7c4c53f7b.png)

## 何时使用
页面需要配置面包屑时

### 使用方法

```javascript
@OceanBreadcrumb([
  { path: "/filecenter", text: "档案系统" },
  { text: "详情" }
])
class Detail extends React.PureComponent {
```