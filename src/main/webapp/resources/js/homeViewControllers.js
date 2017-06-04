DBMigratorApp.controller( 'homeViewController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http, $interval){
	/*var userdata = jsUserData;
	$rootScope.UserData = jsUserData;
	$rootScope.UserData.userRole = "Admin";*/
	$scope.TCArray = [];
	$scope.itemsByPage = 15;
	$scope.DB_TYPE_MONGO = 'Mongo';
	$scope.DB_TYPE_ORACLE = 'Oracle';
	$scope.eventRowCollection = $scope.eventRowCollection ? $scope.eventRowCollection : [];
	$scope.eventStatus = {
			"rowsRead":0,
			"rowsDumped":0,
			"totalRows":0
	};
	$scope.restartEventData = {};
	
	$scope.loadAllEvents = function(){
		//$scope.eventRowCollection = [];
		//$scope.safeEventRowCollection = [];
		$scope.isEventsLoading = true;
		$http({
			method: 'GET',
			url :"getAllEvents",
			params : {userId: $rootScope.UserData.userid}
		}).then(function successCallback(response) {
			$scope.eventRowCollection = response.data;
			$scope.safeEventRowCollection = response.data;
			$scope.isEventsLoading = false;
		},function errorCallback(response) {
			popupFactory.showInfoPopup('Error! :( ', 'Something went wrong while loading Events. [Plese check log]');
		});
		
		
		/*$.ajax({				  
			type :"GET",
			url :"getAllEvents",
			data : "userId="+$rootScope.UserData.userid,
			async: false,			
			success: function(data){
				$scope.eventRowCollection = data;
				$scope.safeEventRowCollection = data;
			},
			fail: function(){
				alert('Nope > Request Failed !!');
			}
		});*/
	},
	
	// This will be called when user selects key attribute in OracletoMongo Sync event 
	$scope.$on('autocomplete-selected', function(e, seltext, selItem){
		if(!$scope.newEvent.keyAttrs){
			$scope.newEvent.keyAttrs = [];
		}
		
		var check = $scope.newEvent.keyAttrs.filter(function(o){return o.attributeName == selItem.attributeName;} );
		if(check.length == 0){
			$scope.newEvent.keyAttrs.push({
				"attributeName": selItem.attributeName,
				"attributeType": selItem.attributeType,
				"isIdentifier": selItem.identifier,
				"columnData": {
					"columnName": selItem.mappedOracleColumn.columnName,
					"columnAlias": selItem.mappedOracleColumn.columnAlias,
					"columnType": selItem.mappedOracleColumn.columnType,
					"tableAlias": selItem.mappedOracleColumn.tableAlias
				},
				"defaultValue": selItem.defaultValue ? selItem.defaultValue : null
			});
		}
		$scope.$broadcast('dataloaded', '');
	});
	
	$scope.removeKeyAttr = function(key){
		var index = $scope.newEvent.keyAttrs.indexOf(key);
		if(index > -1){
			$scope.newEvent.keyAttrs.splice(index, 1);
		}
	}
	
	$scope.loadMappings = function(){
		$scope.isMappingssLoading = true;
		$http({
			method: 'GET',
			url :"getAllMappings"
		}).then(function successCallback(response) {
			$scope.mappingRowCollection = response.data;
			$scope.safeMappingRowCollection = response.data;
			$scope.currentAvailableMappings = response.data;
			$scope.isMappingssLoading = false;
		},function errorCallback(response) {
			popupFactory.showInfoPopup('Error! :( ', 'Something went wrong while loading mappings. [Plese check log]');
		});
		
		
		/*$.ajax({				  
			type :"GET",
			url :"getAllMappings",
			//url :"getMappingByMapType",
			//data : "mapType=MongoToOrcl",
			//data : "mapType=OrclToMongo",
			async: false,			
			success: function(data){
				$scope.mappingRowCollection = data;
				$scope.safeMappingRowCollection = data;
				$scope.currentAvailableMappings = data;
				$timeout(function () {
					$scope.$apply();
				}, 0, false);
			},
			fail: function(){
				alert('Nope > Request Failed !!');
			}
		});*/
	}
	
	$timeout(function () { // You might need this timeout to be sure its run after DOM render.
		popupFactory.showProgressPopup('Loading user log......');
		$scope.loadMappings();
		$scope.loadAllEvents();
		popupFactory.closeProgressPopup();
    }, 0, false);
	
	$scope.createMigrateJob = function(){
		window.location.hash = '#/SelectDB';
	}
	
	$scope.setCurrentMapping = function(mapping, fnCallBack){
		$scope.newEvent.selectedMapping = mapping;
		
		if($scope.newEvent.eventType == 'OrclToMongo' || $scope.newEvent.eventType == 'OrclToMongoSync'){
			$.ajax({				  
				type :"GET",
			    url :"getMappingTables",
			    data : "mappingId="+mapping._id.$oid,
				async: false,			
				success: function(data){
					$scope.newEvent.currentTablesFetchQuery = "&sourceDatabaseName="+data.sourceDbName+"&sourceDatabaseSchema="+data.sourceUserName+"&userName="+$rootScope.UserData.userid;
					$scope.newEvent.availableTables = data.tableList;
					$scope.newEvent.attrName = data.collectionName;
					
					if($scope.newEvent.eventType == 'OrclToMongoSync'){
						$scope.newEvent.topAttributes = data.attributes;
						
						for (var g = 0; g < $scope.newEvent.topAttributes.length; g++) {
							$scope.newEvent.topAttributes[g].label = $scope.newEvent.topAttributes[g].attributeName;
						}
						$scope.autoCompleteValues = $scope.newEvent.topAttributes;
						$scope.$broadcast('dataloaded');
						
						$scope.newEvent.pollingColumns = [];
						for (var k = 0; k < data.attributes.length; k++) {
							if(data.attributes[k].attributeType == "DATE"){
								$scope.newEvent.pollingColumns.push(data.attributes[k])
							}
						}
					}
					
					if(fnCallBack){
						fnCallBack();
					}
				},
				fail: function(){
					alert('Nope > Request Failed !!');
				}
			});
		} else if($scope.newEvent.eventType == 'OrclToMongoGridFs'){
			$scope.newEvent.attrName = mapping.attributeName;
		} else {
			$scope.newEvent.attrName = mapping.collectionName;
		}
	},
	
	$scope.processColumnForPolling = function(column){
		$scope.newEvent.pollingColumn = {
			columnName: column.mappedOracleColumn.columnName,
			columnType: column.mappedOracleColumn.columnType,
			tableAlias: column.mappedOracleColumn.tableAlias
		}
	}
	/*$scope.processColumnForTriggers = function(column){
		$scope.newEvent.triggerColumn = {
				columnName: column.mappedOracleColumn.columnName,
				columnType: column.mappedOracleColumn.columnType,
				tableAlias: column.mappedOracleColumn.tableAlias
		}
	}*/
	
	$scope.editEvent = function(evtData){
		$scope.currentEditingEvent = evtData;
		
		$scope.initEventData();
		$scope.newEvent._id = evtData._id.$oid;
		$scope.newEvent.parallelProcess = evtData.parallelReadInfo ? evtData.parallelReadInfo.processParallel : false;
		if(evtData.parallelReadInfo){
			$scope.newEvent.columnData = evtData.parallelReadInfo.columnData ? evtData.parallelReadInfo.columnData : {};
		}
		$scope.newEvent.name = evtData.eventName;
		$scope.newEvent.desc = evtData.comments ? evtData.comments : 'No Comments Found';
		$scope.newEvent.eventType = evtData.eventType; //$scope.setEventType(evtData.eventType); // For Now 
		if(!$scope.newEvent.loadedMappings || !$scope.newEvent.loadedMappings.length){
			$scope.newEvent.loadedMappings = $scope.currentAvailableMappings;
		}
		//$scope.newEvent.loadedMappings = [];
		for (var i = 0; i < $scope.newEvent.loadedMappings.length; i++) {
			if($scope.newEvent.loadedMappings[i]._id.$oid == evtData.mapId.$oid){
				//$scope.newEvent.selectedMapping = $scope.newEvent.loadedMappings[i];
				$scope.setCurrentMapping($scope.newEvent.loadedMappings[i], function(){
					if(evtData.eventType == "OrclToMongoSync"){
						if(evtData.pollBased){
							if(evtData.pollInfo){
								$scope.newEvent.interval = evtData.pollInfo.interval;
								$scope.newEvent.timeUnit = evtData.pollInfo.timeUnit;
								for (var p = 0; p < $scope.newEvent.pollingColumns.length; p++) {
									var column = $scope.newEvent.pollingColumns[p];
									if(column.mappedOracleColumn.columnName == evtData.pollInfo.pollingColumn.columnName){
										$scope.processColumnForPolling(column);
									}
								}
							}
						}
						
						if(evtData.isTriggerBased){
							$scope.newEvent.triggerBased = evtData.isTriggerBased;
							for (var p = 0; p < $scope.newEvent.pollingColumns.length; p++) {
								var column = $scope.newEvent.pollingColumns[p];
								if(column.mappedOracleColumn.columnName == evtData.triggerColumn.columnName){
									$scope.processColumnForTriggers(column);
								}
							}
						}
					}
				});
				i = $scope.newEvent.loadedMappings.length;
			}
		}
		
		// Set the tableName from Alias that is provided by the events ColumnData [ColumnData only have tablAlias not name]
		if($scope.newEvent.columnData.tableAlias){
			for (var j = 0; j < $scope.newEvent.availableTables.length; j++) {
				if($scope.newEvent.availableTables[j].tableAlias == $scope.newEvent.columnData.tableAlias){
					$scope.newEvent.columnData.tableName = $scope.newEvent.availableTables[j].tableName;
				}	
			}
		}
		
		$scope.newEvent.allowNull = evtData.saveNulls ? evtData.saveNulls : false;
		$scope.newEvent.attrName = evtData.collectionName ? evtData.collectionName : 'No Collection Name';
		$scope.newEvent.parProDeg = evtData.parallelProcessingInfo ? evtData.parallelProcessingInfo.numOfBuckets : 0;
		
		
		$element.find('#createEventModal').modal('show');
		angular.element($element.find('#createEventModal')).scope().setEventType($scope.newEvent.eventType);
	}
	
	$scope.initEventData = function(){
		if(!$scope.newEvent){
			/* Create a newEvent Object and add eventListeres  */
			$scope.newEvent = {
				parallelProcess: false,
				columnData: {}
			};
			
			$scope.newEvent.timeIntervalTypes = ["SECONDS", "MINUTES", "HOURS"]
			
			$scope.$on('relation-column-selected', function(e, selectText, selectType){
				if($scope.newEvent.eventType == "OrclToMongo"){
					$scope.newEvent.columnData.columnName = selectText;
					$scope.newEvent.columnData.columnType = selectType; 
				}
			})
			
			$scope.$watch('newEvent.columnData.columnName', function(newValue, oldValue){
				if(newValue){
					$scope.$broadcast('dataloaded', $scope.newEvent.columnData.columnName);
				}
			});
			
			$scope.$watch('newEvent.columnData.tableName', function(newValue, oldValue){
				if(newValue){
					$scope.fetchingData = true;
					$scope.availableColumns = null;
					$.ajax({				  
						type :"POST",
					    url :"fetchColumnsDetails",
					    data : "tableName="+newValue+$scope.newEvent.currentTablesFetchQuery, //"&sourceDatabaseName="+$scope.UserData.NewData.sourceDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.sourceSchemaName+"&userName="+$scope.UserData.NewData.userId,
						async: true,			
						success: function(data){
							$scope.fetchingData = false;
							$scope.availableColumns = data;
							$scope.$apply();
							if($scope.newEvent.columnData.columnName){
								$scope.$broadcast('dataloaded', $scope.newEvent.columnData.columnName);
							} else {
								$scope.$broadcast('dataloaded');
							}
						},
						fail: function(){
							alert('Nope !!');
						}
					});
				}
			});
		}
	}
	
	/* As there are various watchers and evenListeners on EventData attributes. We need to clear it and not reassign it to new object  */
	$scope.clearEventData = function(){
		$scope.newEvent.parallelProcess = false;
		$scope.newEvent.columnData = {}
		$scope.newEvent.name = '';
		$scope.newEvent.desc = '';
		//$scope.newEvent.loadedMappings = [];
		$scope.newEvent.selectedMapping = null;
		$scope.newEvent.allowNull = false;
		$scope.newEvent.attrName = '';
		$scope.newEvent.parProDeg = 0;
		$scope.newEvent.eventType = '';
		$scope.newEvent.keyAttrs = [];
		$scope.newEvent.topAttributes = null;
		$scope.newEvent.pollBased = false;
		$scope.newEvent.timeUnit = null;
		$scope.newEvent.interval = null;
		$scope.newEvent.pollingColumn = null;
		/*$scope.newEvent.triggerColumn = null;
		$scope.newEvent.triggerBased = false;*/
		$scope.newEvent.pollingColumns = null;
		$scope.newEvent.notificationAlias = '';
	}
	
	$scope.AssignDBPage = function(){
		window.location.hash = '#/DBManager';
	}
	
	$scope.createEvent = function(){
		$scope.initEventData();
		$scope.clearEventData();
		$scope.currentEditingEvent = null;
		$element.find('#createEventModal').modal('show');
	};
	
	$scope.retryEvent = function(row){
		
		$scope.restartEventData = {
			dropCollection: false,
			eventName: row.eventName,
			eventId : row._id.$oid,
			row: row
		};
		$element.find('#restartDetailsPopup').modal('show');
		
		
		//row.status = 'PENDING';
		/*$http({
			method: 'GET',
			url :"retryEvent",
			params : {eventId : row._id.$oid}
		}).then(function successCallback(response) {
			row.status = response.data.status;
		});*/
	}
	
	$scope.retryEventPartial = function(){
		var params = {
			retryFailed: true,
			eventId : $scope.restartEventData.eventId
		};
		$scope.callRetryEvent(params);
	};
	
	$scope.retryEventFull = function(){
		var params = {
			eventId : $scope.restartEventData.eventId,
			retryEntire: true,
			dropCollection: $scope.restartEventData.dropCollection
		};
		$scope.callRetryEvent(params);
	};
	
	$scope.callRetryEvent = function(params){
		$http({
			method: 'GET',
			url :"retryEvent",
			params : params
		}).then(function successCallback(response) {
			$scope.restartEventData.row.status = response.data.status;
			$scope.restartEventData = null;
		});
	};
	
	$scope.viewMigrationStatus = function(row){
		$scope.eventStatus.eventName = row.eventName;
		$element.find('#eventStatusPoup').modal('show');
		$scope.eventStatus.fetching = true;
		$scope.refreshStatus(row);
		
		if(row.status != "COMPLETE" && row.status != "FAILED"){
			$scope.refreshInterval = $interval(function() {
				$scope.refreshStatus(row);
			}, 2000);
		}
	};
	
	$scope.closeStatusPopup = function(){
		if (angular.isDefined($scope.refreshInterval)){
			$interval.cancel($scope.refreshInterval);
			$scope.refreshInterval = undefined;
		}
		$element.find('#eventStatusPoup').modal('hide');
	}
	
	$scope.resetStatusPopup = function(){
		$scope.eventStatus.startTime = "";
		$scope.eventStatus.endTime = "";
		$scope.eventStatus.rowsDumped = 0;
		$scope.eventStatus.rowsRead = 0;
		$scope.eventStatus.totalRows = 0;
	}
	
	$scope.refreshStatus = function(row){
		var eventStatus = $scope.eventStatus;
		$http({
			method: 'GET',
			url :"fetchEventStatus",
			params : {eventId : row._id.$oid}
		}).then(function successCallback(response) {
			$scope.resetStatusPopup();
			eventStatus = $.extend($scope.eventStatus,response.data);
			if(!response.data.rowsDumped && !response.data.totalRows){
				eventStatus.completionPerc = 0;
			} else {
				eventStatus.completionPerc = (response.data.rowsDumped && response.data.totalRows) ? (response.data.rowsDumped / response.data.totalRows * 100).toFixed(2) : 100;
			}
			eventStatus.isActive = (row.status == 'FAILED' || eventStatus.completionPerc == 100) ? false : true; // to show animated progtres bar
			eventStatus.isSuccess = row.status == 'FAILED' ? false : true; // to show green or Red progress bar
			eventStatus.showEndTime = false;
			if(eventStatus.endTime){
				if(eventStatus.totalRows == eventStatus.rowsDumped){
					eventStatus.showEndTime = true;
					var startTime = new Date(eventStatus.startTime.replace('IST',''));
					var endTime = new Date(eventStatus.endTime.replace('IST',''));
					var duration = endTime.getTime() - startTime.getTime();
					eventStatus.duration = $scope.formatTime(duration);
				} 
			}
			eventStatus.fetching = false;
		}, function errorCallback(response) {
			console.log('Fetching event status Unsucssfull......... please refer log : '+response)
		});
	}
	
	$scope.formatTime = function(s) {
		  var miliSec = s % 1000;
		  s = (s - miliSec) / 1000;
		  var secs = s % 60;
		  s = (s - secs) / 60;
		  var mins = s % 60;
		  var hrs = (s - mins) / 60;

		  return hrs + 'Hrs : ' + mins + ' Mins : ' + secs + ' Sec.';	// + miliSec;
	}
	
	$scope.deleteEvent = function(row){
		popupFactory.showConfirmPopup('Are you sure?', 'Do you want to delete event : '+row.eventName+' ?', function (){
			popupFactory.showProgressPopup('Deleting event '+row.eventName+'....');
			$http({
				method: 'GET',
				url :"deleteEvent",
				params : {eventId : row._id.$oid}
			}).then(function successCallback(response) {
				popupFactory.closeProgressPopup();
				var rowIndex = $scope.eventRowCollection.indexOf(row);
				if(rowIndex > -1){
					$scope.eventRowCollection.splice(rowIndex, 1);
				}
			}, function errorCallback(response) {
				console.log('Delete Unsucssfull......... please refer log : '+response)
			});
		});
	};
	
	$scope.cancelEvent = function(row){
		popupFactory.showConfirmPopup('Are you sure?', 'Do you want to cancel event : '+row.eventName+' ?', function (){
			popupFactory.showProgressPopup('Cancelling event '+row.eventName+'....');
			$http({
				method: 'GET',
				url :"cancelEvent",
				params : {eventId : row._id.$oid}
			}).then(function successCallback(response) {
				row.status = response.data.status;
				popupFactory.closeProgressPopup();
			}, function errorCallback(response) {
				console.log('Cancel Unsucssfull......... please refer log : '+response)
			});
		});
	};
	
	$scope.showErrors = function(row){
		popupFactory.showProgressPopup('Fetching erros of event '+row.eventName+'....');
		$http({
			method: 'GET',
			url :"fetchEventErrors",
			params : {eventId : row._id.$oid}
		}).then(function successCallback(response) {
			popupFactory.closeProgressPopup();
			if(!response.data.length){
				response.data = [{
					"errorMessage":"Intentional Error !",
					"threadName":"Dummy test Thread1",
					"fullStackTrace" : ['dfsdfsdfsdfdsf']
				},{
					"errorMessage":"Dummy Error",
					"threadName":"Parallel Thread2"
				}];
			}
			$scope.errorData = response.data;
			$scope.errorData.eventName = row.eventName;
			$element.find('#errorDetailsPopup').modal('show');
		}, function errorCallback(response) {
			console.log('Cant fetch Errors......... please refer log : '+response)
		});
	};
	$scope.deleteMapping = function(row){
		popupFactory.showConfirmPopup('Are you sure?', 'Do you want to delete Mapping : '+row.mapName+' ?', function (){
			popupFactory.showProgressPopup('Deleting event '+row.eventName+'....');
			$http({
				method: 'GET',
				url :"deleteMapping",
				params : {mappingId : row._id.$oid}
			}).then(function successCallback(response) {
				popupFactory.closeProgressPopup();
				var rowIndex = $scope.mappingRowCollection.indexOf(row);
				if(rowIndex > -1){
					$scope.mappingRowCollection.splice(rowIndex, 1);
				}
			}, function errorCallback(response) {
				console.log('Delete Unsucssfull......... please refer log : '+response)
			});
		});
	};
	
	$scope.editMigrationJob = function(row, isCopy){
		console.log('Passsing id : '+row._id.$oid);
		popupFactory.showProgressPopup('Loading JSON....');
		$.ajax({				  
			type :"GET",
		    url :"loadMapping",
		    data : "mappingId="+row._id.$oid,
			async: true,			
			success: function(data){
				if(isCopy){
					delete data._id;
				}
				var strPostData = "sourceDatabaseName="+data.sourceDbName+"&sourceDatabaseSchema="+data.sourceUserName+"&targetDatabaseName="+data.targetDbName+"&targetDatabaseSchema="+data.targetUserName+"&userName="+data.mongoUserName;
				dbUtilities.setCurrentJSON(data);
				$.ajax({				  
					type :"POST",
				    url :"databaseDetails",
				    data : strPostData,
					async: true,			
					success: function(Dbdata){
						//popupFactory.closeProgressPopup(); // This will be closed after rendering of migrate page
						$scope.NewData = Dbdata;
						jsUserData.NewData = Dbdata;
						if(data.mapType == "OrclToMongo"){
							window.location.hash = '#/Migrate';
						} else {
							window.location.hash = '#/MigrateRev';
						}
					},
					fail: function(){
						alert('Nope > Cannot post database details for : !!'+strPostData);
					}
				});
			},
			fail: function(){
				alert('Nope > Fetch event for row id : '+row._id+' Failed !!');
			}
		});
	};
	
	$scope.showMapping = function(row){
		console.log('Passsing id :L '+row._id.$oid)
		$.ajax({				  
			type :"GET",
		    url :"loadMapping",
		    data : "mappingId="+row._id.$oid,
			async: true,			
			success: function(data){
				if(data.mapType == 'OrclToMongo'){
					$scope.processJavaJSONtoMapping(data);
					$scope.mappingTitle = 'DB Mappings for : '+row.mapName;
					$scope.$apply();
					$element.find('#homePageMappingViewer').modal('show');
				} else {
					popupFactory.showInfoPopup('Coming soon!!', 'Mapping structure for Mongo to Oracle mappings will be available soon!');
				}
				
			},
			fail: function(){
				alert('Nope > Request Failed !!');
			}
		});
	};
	
	$scope.fetchTablesMapping = function(sourceTables){
		if(sourceTables.length > 0){
			for (var i = 0; i < sourceTables.length; i++) {
				console.log('Pushing into mapping : '+sourceTables[i].tableName+' > to > '+sourceTables[i].tableAlias);
				if(sourceTables.table){
					$scope.tableMapping[sourceTables[i].table.tableAlias] = sourceTables[i].table.tableName;
					if(sourceTables[i].table.joinedTables && sourceTables[i].table.joinedTables.length > 0){
						$scope.fetchTablesMapping(sourceTables[i].table.joinedTables)
					}
				} else {
					$scope.tableMapping[sourceTables[i].tableAlias] = sourceTables[i].tableName;
					if(sourceTables[i].joinedTables && sourceTables[i].joinedTables.length > 0){
						$scope.fetchTablesMapping(sourceTables[i].joinedTables)
					}
				}
			}
		}
	};
	
	$scope.extractAttributesMapping = function(collection, TCArray, preFixString, isPrimary){
		$scope.fetchTablesMapping(collection.sourceTables);
		console.log($scope.tableMapping);
		for (var i = 0; i < collection.attributes.length; i++) {
			var attr = collection.attributes[i];
			if(attr.columnData){
				TCArray.push({
					column: attr.columnData.columnName,
					tableAlias: attr.columnData.tableAlias,
					attributeName: attr.attributeName,
					attribPreFix: preFixString,
					tableName: $scope.tableMapping[attr.columnData.tableAlias] // Alias are processed before attributes in  fetchTablesMapping
				});
			}
			else if(dbUtilities.isCollectionType(attr.attributeType)){
				var preFix = preFixString ? preFixString+'.'+attr.attributeName : attr.attributeName;
				$scope.extractAttributesMapping(attr, $scope.TCArray, preFix);
			}
		}
		if(isPrimary && collection.identifiers){
			for (var i = 0; i < collection.identifiers.length; i++) {
				var attr = collection.identifiers[i];
				TCArray.push({
					column: attr.columnData.columnName,
					tableAlias: attr.columnData.tableAlias,
					attributeName: attr.attributeName,
					tableName: $scope.tableMapping[attr.columnData.tableAlias] // Alias are processed before attributes in fetchTablesMapping
				});
			}
		}
	},
	
	$scope.processJavaJSONtoMapping = function(jsonData){
		var collection = jsonData.mapObject;
		$scope.tableMapping = {};
		$scope.TCArray.length = 0;
		$scope.extractAttributesMapping(collection, $scope.TCArray, '', true);
		$scope.TCDuplicateArray = $scope.TCArray; // for the st-safe-src of Smart Table in Popup [Enbales sorting safely]
	}
});

DBMigratorApp.controller( 'ceateEventController', function ($scope, popupFactory, devUtilities, $rootScope, $element, $timeout, dbUtilities, $http){
	$scope.submitEvent = function(){
		var eventData = {};
		if($scope.validateEventData(eventData)){
			if($scope.isDebugMode()){
				console.log(eventData);
				return;
			}
			
			$.ajax({				  
				type :"POST",
			    url :"saveEvents",
				async: true,
				data: 'eventJson='+angular.toJson(eventData),
				success: function(data){
					if($scope.currentEditingEvent){
						$scope.currentEditingEvent = data;
						$scope.$apply();
					} else {
						$scope.eventRowCollection.unshift(data);
						$scope.$apply();
					}
					$scope.newEvent.eventType = '';
					$scope.newEvent.loadedMappings.length = 0;
				},
			});
			$element.modal('hide');
		}
	};
	
	$scope.setEventType = function(type){
		$scope.newEvent.eventType = type;
		$scope.loadTypeMappings();
		/*if(type == 'OrclToMongo'){
			
		} else if (type == 'MongoToOrcl'){
			
		}*/
	}
	
	$scope.loadTypeMappings = function(){
		var fetchType = $scope.newEvent.eventType;
		if(fetchType == 'MongoToOrclSync'){
			fetchType = 'MongoToOrcl';
		}
		if(fetchType == 'OrclToMongoSync'){
			fetchType = 'OrclToMongo';
		}
		$http({
			method: 'GET',
			url: 'getMappingByMapType',
			params : {mapType : fetchType}
		}).then(function successCallback(response) {
			$scope.newEvent.loadedMappings = response.data;
			//$element.find('#createEventModal').modal('show');
	   });
	}
	
	$scope.validateEventData = function(eventObj){
		$scope.newEvent.errors = [];
		var valid = true;
		if(!$scope.newEvent.name || $scope.newEvent.name.trim() == ''){
			$scope.newEvent.errors.push('Please specify name');
			valid = false;
		}else{
			eventObj.eventName = $scope.newEvent.name;
		}
		if(!$scope.newEvent.desc || $scope.newEvent.desc.trim() == ''){
			$scope.newEvent.errors.push('Please specify description');
			valid = false;
		} else {
			eventObj.comments = $scope.newEvent.desc;
		}
		if(!$scope.newEvent.attrName || $scope.newEvent.attrName.trim() == ''){
			$scope.newEvent.errors.push('Please specify Collection Name');
			valid = false;
		} else {
			eventObj.collectionName = $scope.newEvent.attrName;
		}
		if(!$scope.newEvent.selectedMapping){
			$scope.newEvent.errors.push('Please specify Mapping');
			valid = false;
		}
		if(!$scope.newEvent.notificationAlias || !$scope.newEvent.notificationAlias.trim()){
			$scope.newEvent.errors.push('Please specify notification alias');
			valid = false;
		} else {
			var aliases = $scope.newEvent.notificationAlias.trim();
			var validAliasRegEx = /^[a-zA-Z0-9]+(\s*,\s*[a-zA-Z0-9]+\d*)*$/g;
			if(!validAliasRegEx.test(aliases)){
				$scope.newEvent.errors.push('Please specify valid notification alias');
				valid = false;
			} else {
				eventObj.notificationAlias = aliases;
			}
		}
		if($scope.newEvent.eventType == 'OrclToMongo'){
			if($scope.newEvent.parallelProcess){
				if(!$scope.newEvent.parProDeg && $scope.newEvent.parProDeg != 0 ){
					$scope.newEvent.errors.push('Please specify Degree of Parellelism');
					valid = false;
				} else if(!Number.isInteger($scope.newEvent.parProDeg)){
					$scope.newEvent.errors.push('Please enter valid Degree of Parallelism (Integer)');
					valid = false;
				}
				if(!$scope.newEvent.columnData.columnName || $scope.newEvent.columnData.columnName.trim() == ''){
					$scope.newEvent.errors.push('Please specify Column Name');
					valid = false;
				}
				if(!$scope.newEvent.columnData.tableName || $scope.newEvent.columnData.tableName.trim() == ''){
					$scope.newEvent.errors.push('Please specify Table Name');
					valid = false;
				}
			}
		}
		
		if($scope.newEvent.eventType == 'MongoToOrclSync'){
			eventObj.rse = $scope.newEvent.allowRSE;
		}
		
		if($scope.newEvent.eventType == 'OrclToMongoSync'){
			if($scope.newEvent.keyAttrs.length == 0){
				$scope.newEvent.errors.push('Please specify at least one key attribute');
				valid = false;
			}
			if($scope.newEvent.pollBased){
				eventObj.pollBased = $scope.newEvent.pollBased;
				eventObj.pollInfo = {};
				if($scope.newEvent.interval){
					if($scope.newEvent.interval <= 0 && ($scope.newEvent.interval % 1) == 0){
						$scope.newEvent.errors.push('Please Enter valid interval for polling');
						valid = false;
					} else {
						eventObj.pollInfo.interval = $scope.newEvent.interval;
					}
				} else {
					$scope.newEvent.errors.push('Please Enter valid interval for polling');
					valid = false;
				}
				if(!$scope.newEvent.timeUnit){
					$scope.newEvent.errors.push('Please select time unit for polling');
					valid = false;
				} else {
					eventObj.pollInfo.timeUnit = $scope.newEvent.timeUnit;
				}
				if(!$scope.newEvent.pollingColumn){
					$scope.newEvent.errors.push('Please select polling column');
					valid = false;
				} else {
					eventObj.pollInfo.pollingColumn = $scope.newEvent.pollingColumn;
				}
			} else {
				eventObj.pollBased = false;
			}
			
			/*if($scope.newEvent.triggerBased){
				eventObj.isTriggerBased = $scope.newEvent.triggerBased;
				if(!$scope.newEvent.triggerColumn){
					$scope.newEvent.errors.push('Please select trigger column');
					valid = false;
				} else {
					eventObj.triggerColumn = $scope.newEvent.triggerColumn;
				}
			}*/
		}
		
		if(!valid){
			return valid;
		}
		eventObj.saveNulls = $scope.newEvent.allowNull ? $scope.newEvent.allowNull : false;
		eventObj.mapId = $scope.newEvent.selectedMapping._id;
		eventObj.mapName = $scope.newEvent.selectedMapping.mapName;
		eventObj.createdBy = $rootScope.UserData.userid;
		if($scope.newEvent.eventType == 'OrclToMongo'){
			eventObj.parallelReadInfo = {
					processParallel : $scope.newEvent.parallelProcess,
	                columnData : $.extend({},$scope.newEvent.columnData),
	                numOfBuckets : Math.round($scope.newEvent.parProDeg)
			};
			delete eventObj.parallelReadInfo.columnData.tableName;
		}
		if($scope.newEvent.eventType == 'OrclToMongoSync'){
			eventObj.keyAttrs = $scope.newEvent.keyAttrs;
		}
		eventObj.eventType = $scope.newEvent.eventType ? $scope.newEvent.eventType : 'OrclToMongo';
		eventObj._id = $scope.newEvent._id;
		
		
		return valid;
	}
});