DBMigratorApp.controller('dbSourceSection', function ($scope, $interval, popupFactory, devUtilities, $rootScope){
	
	// Get Dummy data while debugging -------------------
	if(jsUserData.NewData && devUtilities.isDebugMode()){
		$scope.tables = devUtilities.getDummyTablesData();
	};
	// ----------------------------------------------
	
	$scope.fetchColumnDetails = function(tableName, callbackFn){
		popupFactory.showProgressPopup('Fetching Columns info....');
		$.ajax({				  
			type :"POST",
		    url :"fetchColumnsDetails",
		    data : "tableName="+tableName+"&sourceDatabaseName="+$scope.UserData.NewData.sourceDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.sourceSchemaName+"&userName="+$scope.UserData.NewData.userId,
			async: false,			
			success: function(data){
				popupFactory.closeProgressPopup();
				if(callbackFn){
					callbackFn(data);
				}
			},
			fail: function(){
				alert('Nope !!');
			}
		});
	};
	
	/* NOTE : Storing an event declaration in variable (Angular returns the destroying function when we declare eventListener) so that we can call it on Destroy event to clear the eventListener */
	var cleanRequest =  $rootScope.$on("request-table-data", function(e, tableData){
		// Check if that table already has data
		var tableLog = $.grep($scope.tables, function(tbl){ return tbl.name == tableData.tableName; });
		if(tableLog.length == 0){
			console.log("Something went wrong!! Requested table does not exist in DB");
			return;
		}
		if(tableLog[0].nodes.length == 0){
			$scope.fetchColumnDetails(tableData.tableName, function(data){
				tableLog[0].nodes = data;
				//console.log(tableLog[0]);
				if(tableData.callbackFn){
					tableData.callbackFn({
						tableName : tableData.tableName,
						tableData : data
					});
				}
			})
		} else {
			if(tableData.callbackFn){
				tableData.callbackFn({
					tableName : tableData.tableName,
					tableData : tableLog[0].nodes
				});
			}
		}
		
	});
	
	$scope.$on("$destroy", function() {
		cleanRequest();
    });
	
	$scope.searchTable = function(){	
		var strPostData = "pattern="+($scope.searchString ? $scope.searchString : '')+"&sourceDatabaseName="+$scope.UserData.NewData.sourceDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.sourceSchemaName+"&userName="+$scope.UserData.NewData.userId;
		/*$scope.UserData.NewData 
		$scope.UserData.NewData.sourceDbName
		$scope.UserData.NewData.sourceSchemaName
		$scope.UserData.NewData.userId */
		popupFactory.showProgressPopup('Searching table : '+($scope.searchString ? $scope.searchString : '')+'...');
		$.ajax({				  
			type :"POST",
		    url :"fetchTablesDetails",
		    data : strPostData,
			async: false,			
			success: function(data){
				popupFactory.closeProgressPopup();
				//$scope.tables = [];
				$scope.tables.length = 0;
				for(var i = 0; i < data.length; i ++){
					$scope.tables.push({
						name: data[i],
						nodes: []
					});
				}
			},
			fail: function(){
				popupFactory.showInfoPopup('Error', 'Failed to fetch tables.');
			}
		});
	}
});

DBMigratorApp.controller("DBTableTreeController", function($scope, $rootScope, $interval, popupFactory) {
	$scope.nodeDataFetched = $scope.data.nodes.length ? true : false;
    $scope.deleteNode = function(data) {
        data.nodes = [];
    };
    
    $scope.add = function(data) {
        var post = data.nodes.length + 1;
        var newName = data.name + '-' + post;
        data.nodes.push({name: newName,nodes: []});
    };
    
    /*$scope.copyAllColumns = function(data){
    	if($scope.collections.length == 0){
    		popupFactory.showInfoPopup('No Collection Found!', 'Please create a collection to add attributes.');
    	} else {
    		if(!$scope.nodeDataFetched){ // Load columnNames if not already present
        		popupFactory.showProgressPopup('Fetching Columns and adding attributes...');
        		$scope.fetchColumnDetails(data.name, function(colData){
					$scope.nodeDataFetched = true;
					popupFactory.closeProgressPopup();
					data.nodes = colData;
				});
    	    	
        	}
    		$rootScope.$emit("add-all-columns", data);
    	}
    }*/
    
    $scope.getColumns = function(data, showTree, fnCallback){
    	if(!showTree){
    		showTree = false;
    	}
    	if(!$scope.nodeDataFetched){
    		if(data.nodes.length > 0){
    			$scope.nodeDataFetched = true;
    			data.showtree = !data.showtree;
    			return;
    		}
    		//popupFactory.showProgressPopup('Fetching Columns info....');
    		data.showtree = showTree;
    		$scope.fetchColumnDetails(data.name, function(colData){
				//popupFactory.closeProgressPopup();
				$scope.nodeDataFetched = true;
				data.nodes = colData;
				if(fnCallback){
					return colData;
				}
			});
    	} else {
    		data.showtree = !data.showtree;
    	}
    };
});

DBMigratorApp.controller('expandableNode', function ($scope, $interval, popupFactory, $rootScope){	
	$scope.isExpanded = false;
	$scope.toggleExpansion = function(){
		$scope.isExpanded = !$scope.isExpanded;
	}
});

DBMigratorApp.controller('dbColumnAttributeNode', function ($scope, $interval, popupFactory, $rootScope, $controller){
	angular.extend(this, $controller('expandableNode', {$scope: $scope}));
});
