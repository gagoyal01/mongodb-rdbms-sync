DBMigratorApp.controller('tableRelationsEditorController', function ($scope, $interval, popupFactory, $timeout, dbUtilities, $element){
	$scope.tableName = 'init';
	$scope.availableTables = [];
	$scope.joinTableName= "Select";
	$scope.joinType = "INNER";
	$scope.tablesRelationDataScope = $scope;
	$scope.fetchingData = false;
	$scope.availableOtherTables = [];
	$scope.fetchedtableData = {columns:[], tables: []};
	$scope.filters = [{
		sqlOperation : 'EQ',
		leftHandExpression: null,
		rightHandExpression: null
	}];
	$scope.setTable = function(dtableName){
		$scope.joinTableName = dtableName;
	},
	$scope.setData= function(oData){
		$scope.resetPopup();
		$scope.tableName = oData.tableName ? oData.tableName : 'Unknown Table';
		$scope.joinType = oData.joinType ? oData.joinType : "INNER";
		$scope.fetchedtableData = oData.fetchedTablesData;
		$scope.availableTables = oData.scopeTables ? oData.scopeTables.slice() : [];
		$scope.availableOtherTables = [];
		$scope.parentMode = oData.parentRelationMode ? oData.parentRelationMode : false;
		$scope.isEditorMode = oData.editorMode ? oData.editorMode : false;
		$scope.selfFilterMode = oData.selfFilterMode ? oData.selfFilterMode : false;
		$scope.collectionName = oData.collectionName ? oData.collectionName : 'Unknown Collection';
		$scope.currentFilters = oData.currentFilters ? $scope.copyFilters(oData.currentFilters) : []; // Copy data from currentFilters : copying array will have same object reference Hence need to duplicate objects too !!
		$scope.joinTableName = oData.joinTableName ? oData.joinTableName : "Select";
		$scope.parentTables = oData.parentTables ? oData.parentTables : [];
		$scope.sameParentTableRelationMode = oData.sameParentTableRelationMode ? oData.sameParentTableRelationMode : false;
		$scope.aliasesData = (oData.aliasesData && oData.aliasesData.length) ? oData.aliasesData : []; 
		
		if($scope.parentMode){
			$scope.parentTables = oData.scopeTables.slice();;
		} else if($scope.parentTables.length) { // For self filter mode as well as table relations editor 
			$scope.parentTables = oData.parentTables.slice();;
		}
		
		var j = 0;
		for (var i = 0; i < $scope.availableTables.length; i++) {
			if($scope.availableTables[i] !== oData.tableName || $scope.sameParentTableRelationMode){
				$scope.availableOtherTables[j] = $scope.availableTables[i];
				j++;
			}
		}
		
		if(oData.tableName && ($scope.availableTables.indexOf(oData.tableName) < 0)){
			$scope.availableTables.push(oData.tableName);
		}
		$scope.completeTables = $scope.availableTables.slice();
		
		if(oData.joinTableName){
			$scope.completeTables.push(oData.joinTableName);
		}
		
		$scope.fnCallBack = oData.callbackFn ? oData.callbackFn : null;
		
		if($scope.selfFilterMode){
			if($scope.currentFilters.length == 0){
				/* Left hand expression has to be Always a column */
				$scope.filters.leftHandExpression = {
						columnData : {
							table: '', 
							columnName: ''
						},
						expressionType: "column"
				};
			} else {
				$scope.processTablesForFilters();
				$scope.filters = $scope.currentFilters;
			}
			
		} else {
			if($scope.currentFilters.length > 0){
				$scope.processTablesForFilters();
				$scope.filters = $scope.currentFilters;
			} else {
				// Set compulsory data for the First Filter block
				$scope.filters[0].leftHandExpression = {
						columnData : { table: oData.tableName },
						expressionType: "column"
				};
				
				$scope.filters[0].rightHandExpression = {
						expressionType: "column",
						columnData : {}
				}
			}
		}
		// When this setData() is called after Dragging column from tables panel. The digest cycle need to fire manually !!
		if(oData.needRefresh){
			$scope.$apply();
		}
	};
	
	$scope.copyFilters = function(data){
		var filters = [];
		for (var i = 0; i < data.length; i++) {
			filters.push({
				leftHandExpression: JSON.parse(JSON.stringify(data[i].leftHandExpression)), //$.extend({},data[i].leftHandExpression),
				rightHandExpression: data[i].rightHandExpression ? JSON.parse(JSON.stringify(data[i].rightHandExpression)) : null, //for isNull & isNotNull type filter handling
				logicalOperator: data[i].logicalOperator,
				sqlOperation: data[i].sqlOperation
			});
		}
		return filters;
	}
	
	$scope.processTablesForFilters = function(){
		for (var i = 0; i < $scope.currentFilters.length; i++) {
			$scope.setTableNameFromAlias($scope.currentFilters[i].leftHandExpression);
			$scope.setTableNameFromAlias($scope.currentFilters[i].rightHandExpression);
		}
	}
	
	$scope.setTableNameFromAlias = function(oExpression){
		if(oExpression && oExpression.expressionType == "column"){
			if(!oExpression.columnData.table){
				oExpression.columnData.table = dbUtilities.getTableFromAlias(oExpression.columnData.tableAlias);
				oExpression.columnData.OrigTable = dbUtilities.getTableFromAlias(oExpression.columnData.tableAlias);
				$scope.setDataIntoAliasData(oExpression);
				
			}
		}
	}
	$scope.setDataIntoAliasData = function(oExpression){
		var bAliasSet = false;
		if($scope.aliasesData.length){
			$scope.aliasesData.forEach(function(item, index){
				//console.log('Checking table name : '+item.tableName + ' With required : '+columnData.table);
				if(item.tableName == oExpression.columnData.table){
					//console.log('Match found : seting >> '+item.tableAlias);
					if(item.tableAlias != oExpression.columnData.tableAlias && item.tableAlias.length < oExpression.columnData.tableAlias.length){
						item.tableAlias = oExpression.columnData.tableAlias;
						bAliasSet = true;
					}
				}
			});
		}
		if(!bAliasSet){
			$scope.aliasesData.push({
				tableName : oExpression.columnData.table,
				tableAlias: oExpression.columnData.tableAlias
			})
		}
	}
	
	
	$scope.getFetchedColumns = function(tableName, refScope, fnCallback){
		for (var i = 0; i < $scope.fetchedtableData.columns.length; i++) {
			if($scope.fetchedtableData.columns[i].table == tableName){
				$scope.fetchedtableData.columns[i].columns.sort($scope.sortOnColumnName);
				if(refScope){
					refScope.availableColumns = $scope.fetchedtableData.columns[i].columns;
				}
				if(fnCallback){
					// Give some time for autocomplete dropdown to render
					$timeout(function () { fnCallback(); }, 0, false);
				}
				return $scope.fetchedtableData.columns[i].columns;
			}
		}
		if(tableName && tableName.toLowerCase() !== 'select'){
			var tableColumns = null;
			$scope.fetchingData = true;
			$.ajax({				  
				type :"POST",
			    url :"fetchColumnsDetails",
			    data : "tableName="+tableName+"&sourceDatabaseName="+$scope.UserData.NewData.sourceDbName+"&sourceDatabaseSchema="+$scope.UserData.NewData.sourceSchemaName+"&userName="+$scope.UserData.NewData.userId,
				async: true,			
				success: function(data){
					$scope.fetchingData = false;
					tableColumns = data;
					$scope.fetchedtableData.columns.push({
						table: tableName,
						columns: data
					});
					if(refScope){
						refScope.availableColumns = data;
						refScope.$apply();
					}
					if(fnCallback){
						fnCallback();
					}
				},
				fail: function(){
					alert('Nope !!');
				}
			});
			return tableColumns;
		} else {
			return [];
		}
	}
	
	$scope.sortOnColumnName = function(col1,col2) {
	  if (col1.columnName < col2.columnName)
	    return -1;
	  else if (col1.columnName > col2.columnName )
	    return 1;
	  else 
	    return 0;
	}
	
	$scope.removeFilter = function(Ofilter){
		var filterIndex = $scope.filters.indexOf(Ofilter);
		if(filterIndex >= 0){
			if($scope.parentMode && filterIndex == 0){
				// Don't remove
			} else if(filterIndex == 0 && $scope.filters.length > 1){
				$scope.filters.splice(filterIndex, 1);
				delete $scope.filters[0].logicalOperator;
			} else {
				$scope.filters.splice(filterIndex, 1);
			}
		}
	}
	
	$scope.addNewFilter = function(){
		var filterObj = {
			logicalOperator: 'AND',
			sqlOperation : 'EQ',
			leftHandExpression : {
				expressionType: "column",
				columnData : {
					table: '', 
					columnName: ''
				}
			}
		};
		if($scope.filters.length == 0){
			delete filterObj.logicalOperator;
		}
		if($scope.selfFilterMode){
			/*filterObj.leftHandExpression = {
				expressionType: "column",
			};
			filterObj.rightHandExpression = {
				expressionType: "literal"
			};*/
		}
		$scope.filters.push(filterObj);
	}
	$scope.setRelationsData = function(rData){	
		for (var i = 0; i < rdata.length; i++) {
			rdata[i];
		}
	},
	$scope.DiscardRelation = function(){
		$scope.resetPopup();
	},
	
	/**
	 * Reset all the popup variables to default and clear contents
	 */
	$scope.resetPopup = function(){
		$scope.filters.length = 0;
		$scope.tableName = 'init';
		$scope.availableTables.length = 0;
		$scope.joinTableName= "Select";
		$scope.availableOtherTables.length = 0;
		$scope.filters.push({
			sqlOperation : 'EQ',
			leftHandExpression: null,
			rightHandExpression: null
		})
	};
	$scope.ValidateAndAddRelation= function(){
		if($scope.isValidData()){
			if($scope.fnCallBack){
				$scope.fnCallBack($scope.getCurrentData());
			}
			$scope.resetPopup();
			$('#DBMigratorTableRelationPopup').modal('hide');
		}
	},
	
	$scope.validateExpression = function(expression, anotherExpression){
		var validData = {
			valid: true,
			invalidText: []
		};
		
		if(!expression){
			validData.valid = false;
			validData.invalidText.push('Please define expression ');
		} else {
			if(!expression.isNA){
				if(expression.expressionType == "column"){
					if(!expression.columnData.table || !expression.columnData.table.trim()){
						validData.valid = false;
						validData.invalidText.push('Invalid table name');
					}
					if(!expression.columnData.columnName || !expression.columnData.columnName.trim()){
						validData.valid = false;
						validData.invalidText.push('Invalid column name');
					}
					if(validData.valid && anotherExpression){ // Check if right and left expression both have same table and same column.[ Logically not allowed ]
						if(anotherExpression.expressionType == "column"){
							if(expression.columnData.table.trim() === anotherExpression.columnData.table.trim() && expression.columnData.columnName == anotherExpression.columnData.columnName){
								validData.valid = false;
								validData.invalidText.push('Same column on both right and left expression');
							}
						}
					}
				} else {
					if(expression.expressionType == 'ARRAY'){
						if(!expression.literalValues || !expression.literalValues.length){
							validData.valid = false;
							validData.invalidText.push('Please enter atleast one literal Value');
						}
					} else {
						if(!expression.literalValue || !String(expression.literalValue).trim()){
							validData.valid = false;
							validData.invalidText.push('Please enter Literal Value');
						}
						if(!expression.literalType || !expression.literalType.trim()){
							validData.valid = false;
							validData.invalidText.push('Please enter Literal Type');
						}
					}
				}
			}
			
		}
		return validData;
	},
	
	/**
	Check if all the selections and user inputs are valid
	 */
	$scope.isValidData = function(){
		//return true;
		var isValid = true;
		for (var i = 0; i < $scope.filters.length; i++) {
			var validData = $scope.validateExpression($scope.filters[i].leftHandExpression);
			 if(!validData.valid){
				 var invalidMsg = 
				 $scope.filters[i].leftErrors = validData.invalidText; //"Please provide valid data on left side"
				 isValid = false;
			 } else {
				 delete $scope.filters[i].leftErrors;
			 }
			 validData = $scope.validateExpression($scope.filters[i].rightHandExpression, $scope.filters[i].leftHandExpression);
			 if(!validData.valid){
				 //$scope.filters[i].leftHandExpression = {}
				 $scope.filters[i].rightErrors = validData.invalidText; //"Please provide valid data on Right side"
				 isValid = false;
			 } else {
				 delete $scope.filters[i].rightErrors;
			 }
			 /*if($scope.filters[i].isLHSInvalidColumn()){
				 isValid = false;
			 }*/
		}
		return isValid;
	},
	
	$scope.buildtableAlias = function(columnData){
		//console.log('Inside buildtableAlias');
		var bSetAlias = false;
		if($scope.aliasesData.length){
			//console.log('aliasesData exist : ');
			$scope.aliasesData.forEach(function(item, index){
				//console.log('Checking table name : '+item.tableName + ' With required : '+columnData.table);
				if(item.tableName == columnData.table){
					//console.log('Match found : seting >> '+item.tableAlias);
					columnData.tableAlias = item.tableAlias;
					bSetAlias = true;
				}
			});
		}
		if(!bSetAlias){
			//console.log('Match not found for : '+columnData.table+' > Hence returning NORMAL')
			columnData.tableAlias= dbUtilities.getTableAlias(columnData.table);
		}
	}
	
	$scope.isTableChanged = function(expression){
		if(expression.columnData.OrigTable && expression.columnData.OrigTable == expression.columnData.table){
			return false;
		} else {
			return true;
		}
	}
	
	
	/* This will basically remove tableName and put tableAlias in every columnData block for JSON consistency */
	$scope.processFiltersForJson = function (){
		var processedFilters = [];
		for (var i = 0; i < $scope.filters.length; i++) {
			processedFilters[i] = $.extend({},$scope.filters[i]);
			if(processedFilters[i].leftHandExpression.expressionType == "column"){ // Only calculate table alias if its not there[ Existing Filter already has aliases ]
				//if(!processedFilters[i].leftHandExpression.columnData.tableAlias){
					
					
					if($scope.isTableChanged(processedFilters[i].leftHandExpression)){ // If table is changed then remove the isParentColumn property if exists.
						$scope.buildtableAlias(processedFilters[i].leftHandExpression.columnData);
						if(processedFilters[i].leftHandExpression.columnData.isParentColumn){
							processedFilters[i].leftHandExpression.columnData.isParentColumn = false;
						}
					}
					if(processedFilters[i].leftHandExpression.columnData.OrigTable){
						delete processedFilters[i].leftHandExpression.columnData.OrigTable;
					}
					/*if($scope.aliasesData){
						$scope.aliasesData.forEach(function(item, index){
							
						});
						processedFilters[i].leftHandExpression.columnData.tableAlias= dbUtilities.getTableAlias(processedFilters[i].leftHandExpression.columnData.table);
					} else {
						processedFilters[i].leftHandExpression.columnData.tableAlias= dbUtilities.getTableAlias(processedFilters[i].leftHandExpression.columnData.table);
					}*/
					
				//}
				if(i > 0 && ($scope.parentMode && !$scope.selfFilterMode)){ // Self filter mode and parentMode are not exclusive.
					if($scope.availableOtherTables.indexOf(processedFilters[i].leftHandExpression.columnData.table) > -1){
						processedFilters[i].leftHandExpression.columnData.isParentColumn = true;
					}
				}
				delete processedFilters[i].leftHandExpression.columnData.table;
				delete processedFilters[i].leftHandExpression.columnData.columnAlias;
			}
			if(processedFilters[i].rightHandExpression.isNA){
				//delete processedFilters[i].rightHandExpression.isNA;
				processedFilters[i].rightHandExpression = null;
			} else {
				if(processedFilters[i].rightHandExpression.expressionType == "column"){
			
					delete processedFilters[i].rightHandExpression.columnData.columnAlias;
					//if(!processedFilters[i].rightHandExpression.columnData.tableAlias){ // Only calculate table alias if its not there[ Existing Filter already has aliases ]
						
						
						if($scope.isTableChanged(processedFilters[i].rightHandExpression)){ // If table is changed then remove the isParentColumn property if exists.
							$scope.buildtableAlias(processedFilters[i].rightHandExpression.columnData);
							if(processedFilters[i].rightHandExpression.columnData.isParentColumn){
								processedFilters[i].rightHandExpression.columnData.isParentColumn = false;
							}
						}
						
						if(processedFilters[i].rightHandExpression.columnData.OrigTable){
							delete processedFilters[i].rightHandExpression.columnData.OrigTable;
						}
						//processedFilters[i].rightHandExpression.columnData.tableAlias = dbUtilities.getTableAlias(processedFilters[i].rightHandExpression.columnData.table);
					//}
					if($scope.parentMode && !$scope.selfFilterMode){
						if(i == 0){ // First filter is always left : Child & Right : parent table relation for Parent relation mode
							processedFilters[i].rightHandExpression.columnData.isParentColumn = true;
						} else if($scope.availableOtherTables.indexOf(processedFilters[i].rightHandExpression.columnData.table) > -1){
							processedFilters[i].rightHandExpression.columnData.isParentColumn = true;
						}
					}
					delete processedFilters[i].rightHandExpression.columnData.table;
				} else {
					if(processedFilters[i].rightHandExpression.expressionType == "ARRAY"){
						//processedFilters[i].rightHandExpression.expressionType = "ARRAY"
						delete processedFilters[i].rightHandExpression.literalValue;
					} else {
						delete processedFilters[i].rightHandExpression.literalValues;
					}
				}
			}
		}
		
		return processedFilters;
	},
	
	/* Prepare and send relation JSON from the current UI data */
	$scope.getCurrentData = function(){
		
		var processedFilters = $scope.processFiltersForJson();
		
		if($scope.selfFilterMode){
			var oJSON = {
				filters: processedFilters
			}
		} else {
			var oJSON = {
				mainTableName: $scope.tableName,
				tableName: $scope.joinTableName, // Table to which it is joined
				tableAlias: dbUtilities.getTableAlias($scope.joinTableName), //+'_x', // Table to which it is joined
				joinType: $scope.joinType,
				filters: processedFilters
			};
		}
		
		if($scope.parentMode){
			oJSON.parentMode = true;
		}
		return angular.toJson(oJSON);
	}
});


DBMigratorApp.controller('filterBlockController', function($scope, popupFactory, dbUtilities){
	$scope.isFirstFilter = function(){
		if($scope.filter.logicalOperator){
			return false;
		} else {
			return true;
		}
	};
	
	$scope.isParentTableOnLeftOrRight = function(){
		if($scope.filter.leftHandExpression && $scope.filter.leftHandExpression.columnData){
			if($scope.parentTables.length){
				if($scope.parentTables.indexOf($scope.filter.leftHandExpression.columnData.table) > -1 && $scope.filter.leftHandExpression.columnData.table != ""){ // Some cases I found parentTables array has "" entry
					return true;
				}
			}
		}
		if($scope.filter.rightHandExpression && $scope.filter.rightHandExpression.columnData){
			if($scope.parentTables.length){
				if($scope.parentTables.indexOf($scope.filter.rightHandExpression.columnData.table) > -1 && $scope.filter.rightHandExpression.columnData.table != ""){
					return true;
				}
			}
		}
		return false;
	}
	
	$scope.getLHSColumnType = function(){
		if($scope.filter.leftHandExpression && $scope.filter.leftHandExpression.columnData){
			if($scope.filter.leftHandExpression.columnData.columnType){
				return $scope.filter.leftHandExpression.columnData.columnType;
			}
		} else {
			return 'Not Defined';
		}
	}
	
	$scope.isLHSInvalidColumn = function(){
		if($scope.filter.leftHandExpression && $scope.filter.leftHandExpression.columnData){
			if($scope.filter.leftHandExpression.columnData.columnType){
				return !dbUtilities.isComparableColumnType($scope.filter.leftHandExpression.columnData.columnType);
			}
		}
		return false;
	}
	
	$scope.isMultipleInputRequired = function(){
		if($scope.filter.sqlOperation == "IN" || $scope.filter.sqlOperation == "NIN"){
			return true;
		} else {
			return false;
		}
	}
	
	$scope.isFirstTableRelationFilter = function(){
		if(!$scope.filter.logicalOperator && (!$scope.selfFilterMode || $scope.parentMode)){
			return true;
		} else {
			return false;
		}
	}
	
	$scope.$watch('filter.leftHandExpression.columnData.columnType', function(newValue, oldValue){
		if(!$scope.isFirstTableRelationFilter()){
			if($scope.filter.rightHandExpression && ($scope.filter.rightHandExpression.expressionType == 'literal' || $scope.filter.rightHandExpression.expressionType == 'ARRAY')){
				if(dbUtilities.getColumnMatchLiteralType(newValue)){
					$scope.filter.rightHandExpression.literalType = dbUtilities.getColumnMatchLiteralType(newValue);
					//delete $scope.filter.errorMsg;
				} /*else {
					$scope.filter.errorMsg = 'Column type '+newValue+' is not allowed in comparison !!';
				}*/
			}
		}
	});
	
	$scope.$watch('filter.rightHandExpression.expressionType', function(newValue, oldValue){
		if(newValue == 'literal'){
			if($scope.filter.leftHandExpression && $scope.filter.leftHandExpression.expressionType == 'column'){
				if(dbUtilities.getColumnMatchLiteralType($scope.filter.leftHandExpression.columnData.columnType)){
					$scope.filter.rightHandExpression.literalType = dbUtilities.getColumnMatchLiteralType($scope.filter.leftHandExpression.columnData.columnType);
					//delete $scope.filter.errorMsg;
				} /*else {
					$scope.filter.errorMsg = 'Column type '+$scope.filter.leftHandExpression.columnData.columnType+' is not allowed in comparison !!';
				}*/
				
			}
		}
	});
	
	$scope.$watch('filter.sqlOperation', function(newValue, oldValue){
		if(oldValue){
			if(newValue == "IN" || newValue == "NIN"){
				if($scope.filter.rightHandExpression){
					$scope.filter.rightHandExpression.expressionType = "ARRAY";
				} else {
					$scope.filter.rightHandExpression = {
						expressionType: "ARRAY",
						literalType: "VARCHAR"
					};
				}
				
				if($scope.filter.leftHandExpression && $scope.filter.leftHandExpression.columnData){
					if(dbUtilities.getColumnMatchLiteralType($scope.filter.leftHandExpression.columnData.columnType)){
						$scope.filter.rightHandExpression.literalType = dbUtilities.getColumnMatchLiteralType($scope.filter.leftHandExpression.columnData.columnType);
						//delete $scope.filter.errorMsg;
					} /*else {
						$scope.filter.errorMsg = 'Column type '+$scope.filter.leftHandExpression.columnData.columnType+' is not allowed in comparison !!';
					}*/
					
				};
			} else if(oldValue == "IN" || oldValue == "NIN"){
				$scope.filter.rightHandExpression = null;
			}
		}
		if(newValue == "IS_NULL" || newValue == "IS_NOT_NULL"){
			$scope.filter.rightHandExpression = {};
			$scope.filter.rightHandExpression.isNA =  true;
		} else {
			if($scope.filter.rightHandExpression && $scope.filter.rightHandExpression.isNA){
				delete $scope.filter.rightHandExpression.isNA;
			}
		}
	});
	
	/**
	 * When in selfFilter mode and First filter contains both expression as table.... It is parent table relation !!
	 */
	$scope.isParentRelationFilter = function(){
		if($scope.isFirstFilter() && $scope.selfFilterMode){
			if($scope.filter.rightHandExpression && $scope.filter.rightHandExpression.expressionType == "column"){
				return true;
			}
		}
		return false;
	}
});

DBMigratorApp.controller('genericExpressionController', function($scope, popupFactory){
	$scope.availableColumns = [];
	$scope.subElement = ''; // Define this in individual subClasses
	$scope.$on('relation-column-selected', function(e, selectText, selectType){
		e.stopPropagation();
		if(!$scope.filter[$scope.subElement].columnData){
			$scope.filter[$scope.subElement].columnData = {};
		}
		$scope.filter[$scope.subElement].columnData.columnName = selectText;
		$scope.filter[$scope.subElement].columnData.columnType = selectType;
	});
	
	$scope.updateDate = function(newValue){
		if($scope.filter[$scope.subElement].expressionType == "literal" && $scope.filter[$scope.subElement].literalType == "DATE"){
			$scope.filter[$scope.subElement].literalValue = newValue;
		}else{
			debugger; // Why is date change captured here when it doesnt have date type right now ??
		}
	}
	
	$scope.getTableList = function(){
		if($scope.filter.logicalOperator){
			return $scope.tablesRelationDataScope.availableTables;
		} else {
			return $scope.tablesRelationDataScope.completeTables;
		}
	}
	
	$scope.$on('filter-date-change', function(e, newValue){
		$scope.updateDate(newValue);
		e.stopPropagation();
		//console.log('RightHandExpression Listener to FILTER_DATE_CHANGE : '+newValue);
		/*if($scope.filter[$scope.subElement].expressionType == "literal" && $scope.filter[$scope.subElement].literalType == "DATE"){
			$scope.filter[$scope.subElement].literalValue = newValue;
			e.stopPropagation();
		}
		else{
			debugger; // Why is date change captured here when it doesnt have date type right now ??
		}*/
	});
	
	$scope.isTableSelectDisabled = function(){
		/*if($scope.selfFilterMode){
			if($scope.isParentRelationFilter()){
				return true;
			} else {
				return false;
			}
		}*/
		if(!$scope.filter.logicalOperator && !$scope.selfFilterMode){
			return true;
		}
		return false;
	},
	
	$scope.isColumnSelectDisabled = function(){
		if(!$scope.availableColumns || !$scope.availableColumns.length){
			return true;
		};
		return false;
	},
	
	$scope.isExpressionColumnValue = function(){
		if($scope.filter[$scope.subElement] && $scope.filter[$scope.subElement].expressionType === "column"){
			return true;
		} else {
			return false;
		}
	},
	
	$scope.doesExpressionExist = function(){
		if($scope.filter[$scope.subElement] == null || $scope.filter[$scope.subElement].isNA){
			return false;
		}else {
			return true;
		}
	},
	
	$scope.setExpressionColumnValue = function(){
		$scope.$parent.filter[$scope.subElement] = {
				columnData : {
					table: '', 
					columnName: ''
				},
				expressionType: "column"
		};
		$scope.availableColumns.length = 0;
	},
	$scope.setExpressionLiteralValue = function(){
		$scope.$parent.filter[$scope.subElement] = {
				literalType: "VARCHAR",
				literalValue: "",
				expressionType: "literal"
		};
		$scope.availableColumns.length = 0;
	},
	
	$scope.resetExpression = function(){
		$scope.$parent.filter[$scope.subElement] = null;
		$scope.availableColumns.length = 0;
	}
});

DBMigratorApp.controller('leftExpressionController', function($scope, popupFactory, $element, $controller){
	angular.extend(this, $controller('genericExpressionController', {$scope: $scope}));
	$scope.expressionTitle = "Left Expression",
	$scope.subElement = 'leftHandExpression';	
	
	$scope.$watch('filter.leftHandExpression.literalType', function(newValue, oldValue) {
		if(newValue === 'DATE'){
			if(oldValue){
				$scope.$broadcast('date-reset');
			} else {
				$scope.$broadcast('date-loaded');
			}
			
		} else if(oldValue == 'DATE'){
			$scope.filter.leftHandExpression.literalValue = '';
		}
	});
	
	$scope.$watch('filter.leftHandExpression.columnData.table', function(newValue, oldValue) {
		//$scope.availableColumns = $scope.tablesRelationDataScope.getFetchedColumns(newValue, $scope);
		//debugger;
		if(newValue && newValue !== oldValue){
			if(oldValue){
				$scope.filter.leftHandExpression.columnData.columnName = "";
				if($scope.filter.leftHandExpression.columnData.columnType){
					$scope.filter.leftHandExpression.columnData.columnType = "";
				}
			}
		}
		
		$scope.tablesRelationDataScope.getFetchedColumns(newValue, $scope, function(){
			$scope.$broadcast('dataloaded', $scope.filter.leftHandExpression.columnData.columnName);
		});
		
		var arrAutoComplete = [];
		for (var i = 0; i < $scope.availableColumns.length; i++) {
			arrAutoComplete.push($scope.availableColumns[i].columnName);
		}		
	});
});

DBMigratorApp.controller('rightExpressionController', function($scope, popupFactory, $controller){
	angular.extend(this, $controller('genericExpressionController', {$scope: $scope}));
	$scope.expressionTitle = "Right Expression",
	$scope.subElement = 'rightHandExpression';
	
	$scope.$watch('filter.rightHandExpression.literalType', function(newValue, oldValue) {
		//console.log('Right hand expression >> literalType changed from '+oldValue+' > To >> '+newValue);
		if(newValue === 'DATE'){
			if(oldValue){
				//console.log('Boradcasting >> date-reset');
				$scope.$broadcast('date-reset');
			}/* else {
				console.log('Boradcasting >> date-loaded');
				$scope.$broadcast('date-loaded');
			}*/
		} else if(oldValue == 'DATE'){
			$scope.filter.rightHandExpression.literalValue = '';
		}
	});
			
	$scope.$watch('filter.rightHandExpression.columnData.table', function(newValue, oldValue) {
		if(newValue && newValue !== oldValue){
			if(oldValue){
				$scope.filter.rightHandExpression.columnData.columnName = "";
			}
		}

		$scope.tablesRelationDataScope.getFetchedColumns(newValue, $scope, function(){
			$scope.$broadcast('dataloaded', $scope.filter.rightHandExpression.columnData.columnName);
		});
	});
	
	// For the first Filter the table Name should be restricted to JoinTableName
	$scope.$watch('joinTableName', function(newValue, oldValue) {
		if(!$scope.selfFilterMode && $scope.isFirstFilter){
			if(!$scope.logicalOperator && $scope.filter.rightHandExpression){
				if(!$scope.filter.rightHandExpression.columnData){
					$scope.filter.rightHandExpression.columnData = {};
				}
				$scope.filter.rightHandExpression.columnData.table = $scope.joinTableName;
			}
		}
	});
});

DBMigratorApp.controller('DatepickerCtrl', function($scope) {
	$scope.today = function() {
		$scope.dt = new Date();
	};
	
	if($scope.filter[$scope.subElement]){
		//console.log($scope.filter[$scope.subElement].literalValue);
		if($scope.filter[$scope.subElement].literalValue){
			if($scope.filter[$scope.subElement].literalValue.$date){ // if java returns date as object : {$date:123123123}
				$scope.dt = new Date($scope.filter[$scope.subElement].literalValue.$date);
			} else {
				$scope.dt = new Date($scope.filter[$scope.subElement].literalValue);
			}
			//console.log('Set DATE AS : ');
			//console.log($scope.dt);
		}
	} else {
		$scope.today();
	}
	//

	/*
	 * if($scope.filter[$scope.currentExpression] &&
	 * $scope.filter[$scope.currentExpression].literalType){
	 * $scope.$watch('$scope.filter[$scope.currentExpression].literalType',
	 * function(newValue, oldValue) { if(newValue == 'DATE'){
	 * if($scope.filter[$scope.currentExpression].literalValue){
	 * $scope.dt = new
	 * Date($scope.filter[$scope.currentExpression].literalValue); }
	 * else { $scope.today(); } } }); }
	 */

	$scope.$on('date-reset', function(e){
		//console.log('DATE RESET !!!!'+$scope.subElement);
		$scope.today();
		e.preventDefault();
	});
	
	/*$scope.$on('date-loaded', function(e){
		console.log('DATE LOADED !!!!'+$scope.subElement);
		if($scope.filter[$scope.subElement].literalValue){
			$scope.dt = new Date($scope.filter[$scope.subElement].literalValue);
		}
		e.preventDefault();
	})*/

	$scope.$watch('dt', function(newValue, oldValue) {
		if (newValue != oldValue) {
			// $scope.filter[$scope.currentExpression].literalValue
			// = $scope.dt;
			$scope.$emit("filter-date-change", newValue);
		}
	});

	$scope.clear = function() {
		$scope.dt = null;
	};

	$scope.inlineOptions = {
		/* customClass: getDayClass, */
		minDate : new Date(),
		showWeeks : true
	};

	$scope.dateOptions = {
		/* dateDisabled: disabled, */
		formatYear : 'yy',
		maxDate : new Date(2020, 5, 22),
		minDate : new Date(),
		startingDay : 1
	};

	// Disable weekend selection
	/*
	 * function disabled(data) { var date = data.date, mode =
	 * data.mode; return mode === 'day' && (date.getDay() === 0 ||
	 * date.getDay() === 6); }
	 */

	$scope.toggleMin = function() {
		$scope.inlineOptions.minDate = $scope.inlineOptions.minDate ? null
				: new Date();
		$scope.dateOptions.minDate = $scope.inlineOptions.minDate;
	};

	$scope.toggleMin();

	$scope.open1 = function() {
		$scope.popup1.opened = true;
	};

	/*
	 * $scope.open2 = function() { $scope.popup2.opened = true; };
	 */

	$scope.setDate = function(year, month, day) {
		$scope.dt = new Date(year, month, day);
	};

	$scope.formats = [ 'dd-MMMM-yyyy', 'yyyy/MM/dd',
			'dd.MM.yyyy', 'shortDate' ];
	$scope.format = $scope.formats[0];
	$scope.altInputFormats = [ 'M!/d!/yyyy' ];

	$scope.popup1 = {
		opened : false
	};

	/*
	 * $scope.popup2 = { opened: false };
	 */

	/*
	 * var tomorrow = new Date();
	 * tomorrow.setDate(tomorrow.getDate() + 1); var
	 * afterTomorrow = new Date();
	 * afterTomorrow.setDate(tomorrow.getDate() + 1);
	 * $scope.events = [ { date: tomorrow, status: 'full' }, {
	 * date: afterTomorrow, status: 'partially' } ];
	 */

	function getDayClass(data) {
		var date = data.date, mode = data.mode;
		if (mode === 'day') {
			var dayToCheck = new Date(date)
					.setHours(0, 0, 0, 0);

			for (var i = 0; i < $scope.events.length; i++) {
				var currentDay = new Date($scope.events[i].date)
						.setHours(0, 0, 0, 0);

				if (dayToCheck === currentDay) {
					return $scope.events[i].status;
				}
			}
		}
		return '';
	}
});