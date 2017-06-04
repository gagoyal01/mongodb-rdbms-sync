DBMigratorApp.controller( 'adminViewController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval, DummyDataService){
	var userdata = jsUserData;
	$rootScope.UserData = jsUserData;
	$rootScope.UserData.userRole = "Admin";
	$scope.date = new Date();
	
	$scope.stopNode = function(row){
		
	}, 
	
	$scope.init = function(){
		$scope.loadNodes();
	},
	
	$scope.loadNodes = function(refreshCall){
		$scope.isRefreshing = true;
		$http({
			method: 'GET',
			url: 'getNodeDetails',
			params : {userId : $rootScope.UserData.userid}
			
		}).then(function successCallback(response) {
			if(refreshCall){
				$scope.updateCollapseData(response.data);
			} else {
				$scope.adminNodesCollection = response.data;
			}
			$scope.isRefreshing = false;
	   });
	}
	
	// Restore the states of expansion for nodes after refresh
	$scope.updateCollapseData = function(newData){
		$scope.expandData = {};
		for (var i = 0; i < $scope.adminNodesCollection.length; i++) {
			var oNode = $scope.adminNodesCollection[i];
			$scope.expandData[oNode._id.$oid] = oNode.isExpanded;
		};
		$scope.adminNodesCollection.length = 0;
		newData.forEach(function(node, index){
			node.isExpanded = $scope.expandData[node._id.$oid] ? $scope.expandData[node._id.$oid] : false;
			$scope.adminNodesCollection.push(node);
		})
	}
	
	$scope.refreshNodeData = function(){
		$scope.loadNodes(true);
	}
	
	$scope.init();	
	//$scope.adminNodesCollection = DummyDataService.getAdminPageDummyData();
});


DBMigratorApp.controller( 'serverNodesBlockController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval, DummyDataService){
	if($scope.node.isExpanded == undefined){
		$scope.node.isExpanded = false;
	}
	$scope.isEventsExpanded = true;
	
	if($scope.node && $scope.node.events){
		console.log($scope.node.events);
	}
	
	$scope.expandNodes = function(){
		$scope.node.isExpanded = !$scope.node.isExpanded;
	}
	$scope.expandEvents = function(){
		$scope.isEventsExpanded = !$scope.isEventsExpanded;
	};
	
	$scope.init = function(){
		if($scope.node && $scope.node.events){
			$scope.eventRowCollection = $scope.node.events.slice();
			$scope.safeEventRowCollection = $scope.node.events.slice();
		}
	}
	
	$scope.init();
});

