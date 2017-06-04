DBMigratorApp.controller( 'dbSelectionController', function ($scope, popupFactory, devUtilities, $rootScope){
	//console.log(jsUserData);
	var userdata = jsUserData;
	$rootScope.UserData = jsUserData;
	$scope.DB_TYPE_MONGO = 'Mongo';
	$scope.DB_TYPE_ORACLE = 'Oracle';
	//console.log(JSON.stringify(jsUserData.sourceDbMap));
	//console.log(JSON.stringify(jsUserData.targetDbMap));
	$scope.sections = [{
		title:"Source",
		DataBases: userdata.sourceDbMap,
		dbType: 'Oracle'
	},{
		title: "Target",
		DataBases: userdata.targetDbMap,
		dbType: 'Mongo'
	}];
	
	popupFactory.showSelectionPopup({
		title: 'Select the Migration Type',
		migratTypeSelectMode: true,
		fnCallback: function(choice){
			if(choice == $scope.DB_TYPE_MONGO){
				$scope.conversionType = $scope.DB_TYPE_MONGO;
				$rootScope.UserData.conversionType = $scope.DB_TYPE_MONGO;
				// Defaults are right 
			} else if( choice == $scope.DB_TYPE_ORACLE){
				$scope.conversionType = $scope.DB_TYPE_ORACLE;
				$rootScope.UserData.conversionType = $scope.DB_TYPE_ORACLE;
				$scope.sections = [{
					title:"Source",
					DataBases: userdata.targetDbMap,
					dbType: 'Mongo'
				},{
					title: "Target",
					DataBases: userdata.sourceDbMap,
					dbType: 'Oracle'
				}];
				//$scope.$apply();
			}
		}
	});
	
	/*$scope.$on('$viewContentLoaded', function(){
	    //Here your view content is fully loaded !!
	  });
	*/
	
	
	$scope.validateDbDetails= function(){
		var validData = {
				isValid : true,
				invalidMsg : '<ul>'
		};
		
		var source = $scope.getObjectBy($scope.sections, "title", "Source");
		var target = $scope.getObjectBy($scope.sections, "title", "Target");
		
		if(!source.selectedDBName){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select Source Database</li>'
		}
		if(!source.selectedDBSchema){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select source database Schema</li>'
		}
		if(!target.selectedDBName){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select Taget Database</li>'
		}
		if(!target.selectedDBSchema){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select target database Schema</li>'
		}
		
		/*if($('.db-select:eq(0) option:selected').text() == "Select"){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select Source Database</li>'
		}
		if($('.schema-select:eq(0) option:selected').text() == "Select"){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select source database Schema</li>'
		}
		if($('.db-select:eq(1) option:selected').text() == "Select"){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select Taget Database</li>'
		}
		if($('.schema-select:eq(1) option:selected').text() == "Select"){
			validData.isValid = false;
			validData.invalidMsg += '<li>Please select target database Schema</li>'
		}*/
		
		validData.invalidMsg += '</ul>'
			
		if(!validData.isValid){
			popupFactory.showInfoPopup( 'Error', validData.invalidMsg);
		}
			
		return validData;
	}
	
	$scope.getObjectBy = function(objects, attribute, value){
		var result = objects.filter(function( obj ) {
			  return obj[attribute] == value;
			});
		return result[0];
	}
	
	$scope.onContinue  = function(){
		if(devUtilities.isDebugMode()){
			/* Skipping validation in debug mode */
			var validatedData = {
				isValid : true
			};
		} else {
			var validatedData = $scope.validateDbDetails();
		}
		
		if(validatedData.isValid){
			var source = $scope.getObjectBy($scope.sections, "title", "Source");
			var target = $scope.getObjectBy($scope.sections, "title", "Target");
			var strPostData = "";
			if(devUtilities.isDebugMode()){
				/* Skipping selection and providing default data*/ 
				if($scope.conversionType == $scope.DB_TYPE_MONGO){
					strPostData = "sourceDatabaseName=DV1DMP &sourceDatabaseSchema=DMADM&targetDatabaseName=ccwcddev&targetDatabaseSchema=ccwApp&userName=ssanmukh";
				} else {
					strPostData = "sourceDatabaseName=ccwcddev &sourceDatabaseSchema=ccwApp&targetDatabaseName=DV1DMP&targetDatabaseSchema=DMADM&userName=ssanmukh";
				}
				
			} else {
				strPostData = "sourceDatabaseName="+source.selectedDBName
					+ " &sourceDatabaseSchema="+source.selectedDBSchema
					+ "&targetDatabaseName="+target.selectedDBName
					+ "&targetDatabaseSchema="+target.selectedDBSchema
					+ "&userName=ssanmukh";
			}
			
			popupFactory.showProgressPopup('Fetching Tables info....');
			$.ajax({				  
				type :"POST",
			    url :"databaseDetails",
			    data : strPostData,
				async: true,			
				success: function(data){
					$scope.NewData = data;
					jsUserData.NewData = data;
					popupFactory.closeProgressPopup();
					if($scope.conversionType == $scope.DB_TYPE_MONGO){
						window.location.hash = '#/Migrate';
					} else if ($scope.conversionType == $scope.DB_TYPE_ORACLE){
						window.location.hash = '#/MigrateRev';
					}
					
				},
				fail: function(){
					alert('Nope > Cannot post database details !!');
				}
			});
		}
	}
});


DBMigratorApp.controller('dbSelectionControllerSection', function ($scope, $interval, popupFactory){
    $scope.dbTypeSelected = function (item) {
    	$scope.section.selectedDBName = item;
        $scope.schemas = $scope.dbSchemaMap[$scope.section.selectedDBName];
        
    };
    
    $scope.dbSchemaSelected = function (item) {
        $scope.section.selectedDBSchema = item;
    };
    
	$scope.dbs = [];
	$scope.dbSchemaMap = {};
	$scope.schemas = [];
	for (var key in $scope.section.DataBases) {
	   $scope.dbs.push(key);
	   $scope.dbSchemaMap[key] = $scope.section.DataBases[key];
	}
	
});