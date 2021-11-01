import { remote } from '@/untils/fetch'

class InquireGraphService {
    getDB(params) {
        return remote('/server/inquireGraph/getDB', 'POST', params)
    }

    getRelation(params) {
        return remote(' /server/inquireGraph/getRelation', 'POST', params)
    }

    inquireGraph(params) {
        return remote('/server/inquireGraph/inquireGraph', 'POST', params)
    }

    inquireGraphRelationCount(params) {
        return remote('/server/inquireGraph/inquireGraphRelationCount', 'POST', params)
    }

    loadHistoryQueries(params) {
        return remote('/server/inquireGraph/loadHistoryQueries', 'POST', params)
    }

    customizedQuery(params) {
        return remote('/server/inquireGraph/customizedQuery', 'POST', params)
    }

    addQuery(params) {
        return remote('/server/inquireGraph/addQuery', 'POST', params)
    }

}

export default new InquireGraphService();
