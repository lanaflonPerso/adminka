var app = angular.module('app', ['ui.grid', 'ui.grid.pagination']);

app.controller('TaskCtrl', ['$scope', 'TaskService', function ($scope, TaskService) {
    var paginationOptions = {
        pageNumber: 1,
        pageSize: 100,
        field: 'id',
        sort: 'asc'
    };

    var getTasks = function () {
        TaskService.getTasks(paginationOptions).success(function (data) {
            $scope.gridOptions.data = data.content;
            $scope.gridOptions.totalItems = data.totalElements;
        })
    };

    $scope.send = function() {
        var tasks = typeof $scope.links !== 'undefined' && $scope.links.trim().length > 0  ?
            $scope.links
            .split("\n")
            .map(function (value) {
                return {"payload":value,"info":"", "status":1}
            }) : [];
        TaskService.postTasks(tasks);
    } ;

    getTasks();

    $scope.gridOptions = {
        uiGridAutoResize: false,
    paginationPageSizes: [100],
        paginationPageSize: paginationOptions.pageSize,
        enableColumnMenus: false,
        useExternalPagination: true,
        useExternalSorting: true,
        columnDefs: [
            {name: 'id', width: '5%'},
            {name: 'payload', width: '30%'},
            {name: 'info', cellTooltip: true},
            {name: 'status', width: '5%'}
        ],
        onRegisterApi: function (gridApi) {
            $scope.gridApi = gridApi;
            gridApi.pagination.on.paginationChanged($scope, function (newPage, pageSize) {
                paginationOptions.pageNumber = newPage;
                paginationOptions.pageSize = pageSize;
                getTasks();
            });

            gridApi.core.on.sortChanged($scope, function (grid, sortColumns) {
                if (typeof sortColumns["0"] !== "undefined") {
                    paginationOptions.field = sortColumns["0"].field;
                    paginationOptions.sort = sortColumns["0"].sort.direction;
                    getTasks();
                }
            })
        }
    };

    var socket = new SockJS('notifications');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {

        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/notify', function (notification) {
            console.log(JSON.parse(notification.body).content);
            getTasks();
        });
    })

}]);

app.service('TaskService', ['$http', function ($http) {

    function httpGet(options) {
        var pageNumber = options.pageNumber > 0 ? options.pageNumber - 1 : 0;
        return $http({
            method: 'GET',
            url: 'task/get?page=' + pageNumber + '&size=' + options.pageSize + '&sortField=' + options.field + '&sortDirection=' + options.sort
        });
    }

    function httpPost(tasks) {
        return $http({
            method: 'POST',
            url: 'task/save',
            data:tasks
        });
    }

    return {
        getTasks: httpGet,
        postTasks: httpPost
    };
}]);
