# 关系图谱

## 对应域名
-- 待定

## 相关人员
PM：xx
前端开发：徐旭彬
devops：待定

## 开发
- 修改本地hosts：127.0.0.1 local.dev.bkjk-inc.com
- npm install
- 启动webpack-dev-server： npm run start:client
- 启动node：npm start

## 部署
npm run build:ENV && npm run start:env
build结果会在public目录下
该项目会在node中集成

## 关于前端Ocean的使用
见src中各个目录下的readme.md

## 技术栈
- react16、react-router4、antd 3.10
- koa2
- 放弃使用redux，改用封装了的context hoc，项目更轻便