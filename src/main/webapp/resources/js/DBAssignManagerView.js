DBMigratorApp.controller( 'DBAssignManagerController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval){
	$scope.oracleDbList = [];
	$scope.dbData = {};
	$scope.mongoDbList = [];
	$scope.newAssignData = {};
	$scope.currentEdit = {};
	$scope.init = function(){
	}
	
	$scope.addDBToUser = function(){
		
		/*if(!$scope.validateDBBlocks()){
			return;
		}*/
		// TODO : Append the $scope.newAssignData.dbData data to save AJAX call and UI
		
		//$scope.currentUserData.targetDbMap
		//$scope.currentUserData.sourceDbMap
		//saveUserDetails
		
		var sendJSON = { 
			'_id': $scope.currentUserData.userid, 
		};
		
		if($scope.newAssignData.type == 'Oracle'){
			sendJSON.sourceDbMap = $scope.newAssignData.dbData;
			sendJSON.targetDbMap = [];
		} else {
			sendJSON.targetDbMap = $scope.newAssignData.dbData;
			sendJSON.sourceDbMap = [];
		}
		
		console.log(angular.toJson(sendJSON));
		
		$http({
			method: 'GET',
			url :"saveUserDetails",
			params : {userDetailsStr : angular.toJson(sendJSON)}
		}).then(function successCallback(response) {
			//response.data
			/*{"_id":"ssanmukh","sourceDbMap":[{"dbName":"TS1DMP","schemas":["CPCADM","XXCFG_Q2O_U","XXCQO_CPC_U"]
			}],"targetDbMap":[]}*/
			if(response.data.sourceDbMap.length){
				for (var i = 0; i < response.data.sourceDbMap.length; i++) {
					var mappedDB = response.data.sourceDbMap[i];
					if(!$scope.currentUserData.sourceDbMap[mappedDB.dbName]){
						$scope.currentUserData.sourceDbMap[mappedDB.dbName] = [];
					}
					for (var j = 0; j < mappedDB.schemas.length; j++) {
						if($scope.currentUserData.sourceDbMap[mappedDB.dbName].indexOf(mappedDB.schemas[j]) == -1){
							$scope.currentUserData.sourceDbMap[mappedDB.dbName].push(mappedDB.schemas[j]);
						}
					}
				}
			}
			if(response.data.targetDbMap.length){
				for (var i = 0; i < response.data.targetDbMap.length; i++) {
					var mappedDB = response.data.targetDbMap[i];
					if(!$scope.currentUserData.targetDbMap[mappedDB.dbName]){
						$scope.currentUserData.targetDbMap[mappedDB.dbName] = [];
					}
					for (var j = 0; j < mappedDB.schemas.length; j++) {
						if($scope.currentUserData.targetDbMap[mappedDB.dbName].indexOf(mappedDB.schemas[j]) == -1){
							$scope.currentUserData.targetDbMap[mappedDB.dbName].push(mappedDB.schemas[j]);
						}
					}
				}
			}
			$element.find('#addDbAndSchemaPopup').modal('hide');
		});
	}
	
	$scope.loadUserData = function(){
		// TODO : Load ajax using >>>> $scope.newAssignData.userId
		$http({
			method: 'GET',
			url :"getUser",
			params : {userId : $scope.currentEdit.userInputId}
		}).then(function successCallback(response) {
			if(response.data.validUser){
				$scope.currentUserData = response.data;
				if(!$scope.currentUserData.sourceDbMap){
					$scope.currentUserData.sourceDbMap = {};
				}
				if(!$scope.currentUserData.targetDbMap){
					$scope.currentUserData.targetDbMap = {};
				}
			} else {
				popupFactory.showInfoPopup('Error!!', 'User not valid. Please check again !.');
			}
		});
	}
	
	/*$scope.sortDBByTypes = function(loadedDB){
		for (var i = 0; i < loadedDB.length; i++) {
			if(loadedDB[i].dbType == "Oracle"){
				$scope.oracleDbList.push(loadedDB[i]);
			} else {
				$scope.mongoDbList.push(loadedDB[i]);
			}
		}
	};*/
	
	$scope.addOrclBox = function(typeStr){
		$scope.newAssignData = {
			type: 'Oracle',
			dbData: [{
				dbName: '',
				schemas: []
			}]
		};
		$scope.showAddDBPopup();
	},
	
	$scope.addMongoBox = function(typeStr){
		$scope.newAssignData = {
			type: 'Mongo',
			dbData: [{
				dbName: '',
				schemas: []
			}]
		};
		$scope.showAddDBPopup();
	},
	
	$scope.addMoreBlock = function(){
		$scope.newAssignData.dbData.push({
			dbName: '',
			schemas: []
		})
	}
	
	$scope.showAddDBPopup = function(typeStr){
		$scope.newAssignData.dbNameMap = null;
		$scope.newAssignData.loadedList = [];
		if($scope.currentUserData.completeDBList){
			$scope.currentUserData.completeDBList.forEach(function(dbData, index){
				if(dbData.dbType && dbData.dbType == $scope.newAssignData.type){
					$scope.pushNewAssignDB(dbData);
				}
			})
			$element.find('#addDbAndSchemaPopup').modal();
		} else {
			popupFactory.showProgressPopup('Loading DB\'s....');
			$http({
				method: 'GET',
				url :"getConnectionDetails"
			}).then(function successCallback(response) {
				//debugger;
				$scope.currentUserData.completeDBList = response.data;
				response.data.forEach(function(dbData, index){
					if(dbData.dbType && dbData.dbType == $scope.newAssignData.type){
						$scope.pushNewAssignDB(dbData);
					}
				});
				console.log($scope.newAssignData);
				popupFactory.closeProgressPopup();
				$element.find('#addDbAndSchemaPopup').modal();
			});
		}
		
	}
	
	$scope.pushNewAssignDB = function(dbData){
		if(!$scope.newAssignData.dbNameMap){
			$scope.newAssignData.dbNameMap = {};
		}
		if(!$scope.newAssignData.dbNameMap[dbData.dbName]){
			var dbObject = {
					dbName: dbData.dbName,
					userNames: [dbData.userName]
				};
			$scope.newAssignData.dbNameMap[dbData.dbName] = dbObject;
			$scope.newAssignData.loadedList.push(dbObject);
		} else {
			$scope.newAssignData.dbNameMap[dbData.dbName].userNames.push(dbData.userName);
		}
	}
	
	$scope.validateDBBlock = function(dBlock){
		var err = [];
		if(!dBlock.dbName || !dBlock.dbName.trim()){
			err.push('Please select valid DB name');
		}
		if(!dBlock.schemas || !dBlock.schemas.length){
			err.push('Please select atleast one Schema');
		}
		if(!err.length){
			delete dBlock.errors;
			return true;
		} else {
			dBlock.errors = err.slice();
			return false;
		}
	}
	
	$scope.validateData = function(){
		if(!$scope.newAssignData.userId || !$scope.newAssignData.userId.trim()){
			popupFactory.showInfoPopup('Error!!', 'Please enter valid user id !.');
			return false;
		}
		var bValid = true;
		var InvalidMsg = []
		for (var i = 0; i < $scope.newAssignData.orclData.length; i++) {
			var orclElement = $scope.newAssignData.orclData[i];
			if(!$scope.validateDBBlock(orclElement)){
				bValid = false;
			}
		}
		for (var i = 0; i < $scope.newAssignData.mongoData.length; i++) {
			var mongoElement = $scope.newAssignData.mongoData[i];
			if(!$scope.validateDBBlock(mongoElement)){
				bValid = false;
			}
		}
		return bValid;
	}
	
	$scope.validateAndSubmitAssign = function(){
		if($scope.validateData()){
			popupFactory.showInfoPopup('Java Migrate JSON : ', "<pre>"+JSON.stringify(JSON.parse(angular.toJson($scope.newAssignData)), undefined, 2)+"</pre>");
		}
		
		//popupFactory.showInfoPopup('Java Migrate JSON : ', "<pre>"+angular.toJson($scope.newAssignData)+"</pre>");
		/*popupFactory.showInfoPopup('Success!!', 'Will be saved soon !!!.', false, function(){
			$scope.initFormParametres();
			$scope.$apply();
			//window.location.hash = '#/Home';
		});*/
	}
	
	$scope.init();
});

DBMigratorApp.controller( 'DBAssignBlockController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval){
	
	$scope.autoCompleteValues = [];
	
	$scope.updateSchemas = function(schemas){
		$scope.currentData.schemas.length = 0;
		$scope.autoCompleteValues.length = 0;
		schemas.forEach(function(schema, index){
			$scope.autoCompleteValues.push({
				value: schema,
				label: schema
			})
		});
		$scope.$broadcast('dataloaded');
	};
	
	$scope.removeSchema = function(schema){
		var schemaIndex = $scope.currentData.schemas.indexOf(schema);
		if(schemaIndex > -1){
			$scope.currentData.schemas.splice(schemaIndex, 1);
		}
	}
	
	//$scope.$on('autocomplete-selected', item.label, item);
	
	$scope.$on('autocomplete-selected', function(e, seltext, selItem){
		if($scope.currentData.schemas.indexOf(seltext) == -1){
			$scope.currentData.schemas.push(seltext);
		}
		e.stopPropagation();
		$scope.$broadcast('dataloaded', "");
	});
});
DBMigratorApp.controller( 'DBCreateConnectionController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval){
	
	$scope.init = function(){
		$scope.newConectionData = {
			dbName: ''
		};
	};
	
	$scope.testConnection = function(){
		if($scope.checkConnection()){
			popupFactory.showInfoPopup('Success!!', 'Connection '+$scope.newConectionData.dbName+' is active !.');
		} else {
			popupFactory.showInfoPopup('Error!!', 'Invalid connection. Please check details !.');
		}
	};
	
	$scope.checkConnection = function(){
		if(!$scope.newConectionData.dbName || !$scope.newConectionData.dbName.trim()){
			popupFactory.showInfoPopup('Error!!', 'Please enter valid connection name !.');
			return false;
		}
		return true;
	}
	
	$scope.SaveConnection = function(){
		if($scope.checkConnection()){
			popupFactory.showInfoPopup('Success!!', 'Connection '+$scope.newConectionData.dbName+' is Added !.');
		}
	}
	
	$scope.init();
});
