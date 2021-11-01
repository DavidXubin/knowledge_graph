# knowledge_graph
关系图谱平台：
该关系图谱服务平台旨在帮助用户和app应用程序方便地查询和分析关系图谱的数据，支持OLTP查询和OLAP分析，OLTP查询包括各种点查功能，例如查询属性满足特定条件的实体和关系，查询实体的N阶邻居节点以及任意业务场景的查询， OLAP分析包括图数据的社区发现，连通分量，最短路径等等。该服务平台提供了查询的界面支持，可视化嵌入，以及Restful API的调用方式，查询逻辑和算法可以插件形式动态添加入服务平台，目前其对接的图数据库是Janusgraph，并可方便扩展对接更多的图数据库。

kgraph_fe
图谱前端，基于KOA，nodejs, javascript

kgraph_service
图谱后端，基于java, groovy, 可动态添加任意场景的查询语言
