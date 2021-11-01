var getUrlQuery = function getUrlQuery(sParam) {
    const sPageURL = decodeURIComponent(window.location.search.substring(1))
    const sURLVariables = sPageURL.split('&')

    for (var i = 0; i < sURLVariables.length; i++) {
        const sParameterName = sURLVariables[i].split('=');

        if (sParameterName[0] === sParam) {
            return sParameterName[1] === undefined ? true : sParameterName[1];
        }
    }
};

var getUrlParameter = function getUrlParameter() {
    const pathname = decodeURIComponent(window.location.pathname)

    const p = pathname.lastIndexOf("/")
    if (p < 0) {
        return null
    }

    return pathname.substring(p + 1)
};