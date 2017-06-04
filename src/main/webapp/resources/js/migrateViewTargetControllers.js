DBMigratorApp.controller('dbCollectionInstanceController', function ($scope, $rootScope, $interval, popupFactory, dbUtilities, $element, $timeout){
	//$scope.attribEditMode = false;
	$scope.isExpanded = true;
	$scope.selectedAttribType = "";
	$scope.attribTypes = [];
	$scope.collectionRename = false;
	
	if(!$scope.element.currentAttribTables){
		$scope.element.currentAttribTables = [];
	}
	
	if(!$scope.element.currentTableRelations){
		$scope.element.currentTableRelations = [];
	}
	
	$scope.checkIdentifiers = function(){
		if($scope.element.attrs.length){
			for (var i = 0; i < $scope.element.attrs.length; i++) {
				if($scope.element.attrs[i].isIdentifier){
					return true;
				}
			}
		}
		return false;
	}
	
	$scope.toggleExpansion = function(){
		$scope.isExpanded = !$scope.isExpanded;
	}
	
	$scope.renameCollection = function(){
		$scope.prevName = $scope.element.attributeName;
		$scope.collectionRename = true;
	}
	
	$scope.renameDone = function(){
		$scope.collectionRename = false;
	}
	
	$scope.cancelRename = function(){
		$scope.element.attributeName = $scope.prevName;
		$scope.collectionRename = false;
	}
	
	if($element.parents('.mongo-collection-instance').length == 0){
		$scope.isParentNode = true;
	} else {
		$scope.isParentNode = false;
	}
	
	var cleanEventRequest = $scope.$on("request-remove-collection", function(e, collection){
		if(collection != $scope.element){
			var iIndex = $scope.element.attrs.indexOf(collection);
			if(iIndex > -1){
				popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove collection '+collection.attributeName+' ?', function (){
					if($scope.element.collectionType!='GridFs'){
						$scope.element.attrs.splice(iIndex, 1);
					}else{
						$scope.element.attrs.indexOf(collection)>-1 ? $scope.element.attrs.indexOf(collection).splice(iIndex, 1) : '';
						//$scope.element.attrs.columnData = null;
					}
					e.stopPropagation();
					$scope.$apply();
				});
			}
		}
	});
	
	$scope.addRefAttribute = function(colData){
		if(!$scope.element.referencedColumns){
			$scope.element.referencedColumns = [];
		};
		if($scope.element.referencedColumns.indexOf(colData) < 0){
			$scope.element.referencedColumns.push({
				"columnName": colData.columnName,
				"columnType": colData.columnType,
				"tableAlias": colData.tableAlias,
				"tableName": colData.tableName
			})
		};
	}
	
	$scope.createSubCollection = function(collectionName, avoidUpdateCall){
		for (var i = 0; i < $scope.element.attrs.length; i++) {
			if($scope.element.attrs[i].attributeName === collectionName){
				popupFactory.showInfoPopup('Nothing to select!', 'Attribute with same name already exist.');
				return;
			}
			
		}
		var CollectionBlock = {
				attributeName: collectionName,
				attributeType: "AUTO", 
				attrs: [],
				sourceTables: [],
				currentAttribTables: [],
				filters: []
			};
    	$scope.element.attrs.push(CollectionBlock);
    	if(!avoidUpdateCall){
    		$scope.$apply();
    	}
	}
	
	$scope.AddSubCollection = function(){
		if($scope.collectionType == 'GridFs' || $scope.isGridSubcollectionAdded){
			return;
		}
		popupFactory.getInputPopup('Enter collection name : ', $scope.createSubCollection);
	}
	
	$scope.addMetaAttributes = function(collectionName, avoidUpdateCall){
		if($scope.isGridSubcollectionAdded){
			return;
		}
		for (var i = 0; i < $scope.element.attrs.length; i++) {
			if($scope.element.attrs[i].attributeName === collectionName){
				popupFactory.showInfoPopup('Nothing to select!', 'Attribute with same name already exist.');
				return;
			}
		}
		$scope.collectionType = 'GridFs';
		$scope.isGridSubcollectionAdded = true;
		var CollectionBlock = {
				isGridSpecific : true,
				attributeName: 'META_ATTRIBUTES',
				attributeType: "AUTO", 
				attrs: [],
				sourceTables: [],
				currentAttribTables: [],
				filters: []
			};
    	$scope.element.attrs.push(CollectionBlock);
    	if(!avoidUpdateCall){
    		$timeout(function () { $scope.$apply(); }, 0, false);
    	}
	}
	
	$scope.AddBlankAttribute = function(){
		var xferData = {
			label: 'ATTRIB_NAME',
			type: 'NUMBER'
		};
		$scope.$emit("add-collection-attribute-popup", xferData, $scope, true);
	}
	
	$scope.editIdentifiers = function(){
		$element.find('#selectIdentifierPopup').modal('show');
		/*if($scope.element.attrs && $scope.element.attrs.length == 0){
			popupFactory.showInfoPopup('Nothing to select!', 'There are no attributes in collection to mark as identifiers');
			return;
		}
		popupFactory.showSelectIdentifiersPopup({
			element: $scope.element,
			title: 'Choose Identifiers'
		});*/
	}
	
	$scope.$on("remove-referenced-column", function(e, attrib) {
		if($scope.element.referencedColumns && $scope.element.referencedColumns.length){
			for (var i = 0; i < $scope.element.referencedColumns.length; i++) {
				var refColumn = $scope.element.referencedColumns[i];
				if(refColumn.columnName == attrib.columnData.columnName && refColumn.tableAlias == attrib.columnData.tableAlias){
					$scope.element.referencedColumns.splice(i, 1);
					e.stopPropagation();
				}
			}
		}
    });
	
	/* NOTE : Remove all the event Listeners attached to the scope on destroy */
	$scope.$on("$destroy", function() {
		cleanEventRequest();
	});
	
	$scope.getCurrentRelations = function(arrData, sourceTables, parentTable){
		if(sourceTables.length > 0){
			for (var i = 0; i < sourceTables.length; i++) {
				if(sourceTables[i].joinedTables && sourceTables[i].joinedTables.length > 0){
					for (var j = 0; j < sourceTables[i].joinedTables.length; j++) {
						arrData.push({
							label1: sourceTables[i].tableName,
							label2: sourceTables[i].joinedTables[j].tableName,
							value: {
								filters: sourceTables[i].joinedTables[j].filters.slice(),
								joinType: sourceTables[i].joinedTables[j].joinType,
								joinedTableName : sourceTables[i].tableName,
								mainTable : sourceTables[i].joinedTables[j].tableName
							}
						});
					}
					$scope.getCurrentRelations(arrData, sourceTables[i].joinedTables);
				}
			}
		}
	}
	
	$scope.editTableRelations = function(){
		var currentRelationTables = [];
		$scope.getCurrentRelations(currentRelationTables, $scope.element.sourceTables);
		if(currentRelationTables.length == 0){
			popupFactory.showInfoPopup('Nothing Found!', 'Current collection '+$scope.element.attributeName+' doesn\'t have any table relations defined !!');
			return;
		}
		popupFactory.showSelectionPopup({
			title: 'Select the table relation to edit',
			options: currentRelationTables,
			fnCallback: function(value){
				popupFactory.getTableRelationPopup({
					currentFilters: value.filters,
					tableName: value.mainTable,
					joinTableName : value.joinedTableName,
					joinType: value.joinType,
					editorMode : true,
					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
					callbackFn: function(jsData){
						$scope.addSourceTable(JSON.parse(jsData));
				}});
			}
		})
	}
	
	$scope.addAllColumns = function(colData){
		if($scope.checkCurrentTableName(colData.name)){
			if($scope.isParentRelationRequired(colData.name)){
				var upperParentWithTables = $scope.upperParentsWithAttributes();
				var upperParentTables = upperParentWithTables.tables;
				var upperParentSourceTables = upperParentWithTables.sourceTables ? upperParentWithTables.sourceTables : null;
    			if(upperParentTables){
    				if(upperParentTables.length == 1 && (upperParentTables.indexOf(colData.name) > -1)){ // Parent has same table and its only table parent has 
    					
    					$scope.checkSelfJooinedTableCounter();
    					$scope.confirmSameParentTableRelation({tableName: colData.name, existingTableAliases: $scope.element.sourceTables, existingTableAliases: upperParentSourceTables}, function(jsData){
    						if(jsData){
    							var parentChildRelationJSON = JSON.parse(jsData);
        						if(parentChildRelationJSON){
        							for (var i = 0; i < parentChildRelationJSON.filters.length; i++) {
        								$scope.element.filters.push(parentChildRelationJSON.filters[i]);
        							}
        						}
        						$scope.addColumns(colData, dbUtilities.getTableAlias(colData.name)+"_E"+$scope.$id+$scope.element.extTableCounter);
        						dbUtilities.setSameNameFromAlias(colData.name, dbUtilities.getTableAlias(colData.name)+"_E"+$scope.$id+$scope.element.extTableCounter);
        						if(!$scope.element.sourceTables){
        							$scope.element.sourceTables = [];
        						}
        						if(parentChildRelationJSON.filters.length){
        							$scope.element.sourceTables.push({
    	            					"tableName": colData.name,
    	            		        	"tableAlias": dbUtilities.getTableAlias(colData.name)+"_E"+$scope.$id+$scope.element.extTableCounter,
    	            				});
        						}
        						parentChildRelationJSON = null;
    						}
    						else {
    							colData.nodes.forEach(function(element, index, array){
            						upperParentWithTables.collection.addRefAttribute({
            							"tableAlias" : dbUtilities.getTableAlias(colData.name),
            							"tableName" : colData.name,
            							"columnName" : element.columnName,
            							"columnType" : element.columnType
            						});
            					});
    							$scope.processParentTableRelatedNodes(colData.nodes);
    							$scope.addColumns(colData);
    						}
        					
        					$scope.pushCurrentTableName(colData.name);
        					$timeout(function () { $scope.$apply(); }, 0, false);
    					});
    					
    					
    					//$scope.addSourceTableIfDoesntExist(colData.name);
    				} else {
    					popupFactory.getTableRelationPopup({
        					tableName: colData.name,
        					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
        					parentRelationMode: true,
        					scopeTables: upperParentTables,
        					callbackFn: function(jsData){
        						$scope.element.filters = JSON.parse(jsData).filters.slice(0);
        						if(upperParentSourceTables && upperParentSourceTables.length){
        							/* Table relations editor returns aliases using dbUtilities.getTableAlias function which is unaware of repeated tables. Need to fix this. */
        							for (var j = 0; j < upperParentSourceTables.length; j++) {
        								$scope.element.filters.forEach(function(filter, index){
            								if(index == 0){ // For the first one only RHS is from parent ( as per normal relation mode )... Remaining filters can be complex
                								if(filter.rightHandExpression.columnData.tableAlias == dbUtilities.getTableAlias(upperParentSourceTables[j].tableName)){
                									filter.rightHandExpression.columnData.tableAlias = upperParentSourceTables[j].tableAlias;
                								}
            								} else {
            									if(filter.rightHandExpression.expressionType == "column"){
                    								if(filter.rightHandExpression.columnData.tableAlias == dbUtilities.getTableAlias(upperParentSourceTables[j].tableName)){
                    									filter.rightHandExpression.columnData.tableAlias = upperParentSourceTables[j].tableAlias;
                    								}
            									} else if(filter.leftHandExpression.expressionType == "column"){
                    								if(filter.leftHandExpression.columnData.tableAlias == dbUtilities.getTableAlias(upperParentSourceTables[j].tableName)){
                    									filter.leftHandExpression.columnData.tableAlias = upperParentSourceTables[j].tableAlias;
                    								}
            									}
            								}
                						})
        							}
        						};
        						$scope.addSourceTableIfDoesntExist(colData.name);
        						$scope.pushCurrentTableName(colData.name);
        						$scope.addColumns(colData);
        				}});
    				}
    				
    			} else {
    				popupFactory.showInfoPopup('Error !!', "No parent with attribute found. Please add attribute to atleast one parent collection.")
    			}
			} else {
				$scope.addColumns(colData);
				$scope.pushCurrentTableName(colData.name);
				$scope.addSourceTableIfDoesntExist(colData.name);
			}
		} else {
			popupFactory.getTableRelationPopup({
				tableName: colData.name, 
				fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
				scopeTables: $scope.element.currentAttribTables,
				callbackFn: function(jsData){
					$scope.addSourceTable(JSON.parse(jsData));
					$scope.pushCurrentTableName(colData.name);
					$scope.addColumns(colData);
			}});
		}
	}
	
	$scope.addColumns = function(colData, preDefTableAlias){
		var nodes = colData.nodes;
		var strDuplicate = '';
		for (var i = 0; i < nodes.length; i++) {
			var nodeType = dbUtilities.getDefaultAttributeType(nodes[i].columnType);
			var supportedTypes = dbUtilities.getMongoAttrTypes(nodes[i].columnType);
			if($scope.element.attrs.length != 0){
				var tableIndexes = $.grep($scope.element.attrs, function(e){ return e.attributeName == nodes[i].columnName; });
				if(tableIndexes.length == 0){
					$scope.element.attrs.unshift({
						columnData : {
							columnName: nodes[i].columnName,
							tableAlias: preDefTableAlias ? preDefTableAlias : dbUtilities.getTableAlias(colData.name),
							columnType: nodes[i].columnType,
							isParentColumn: nodes[i].isParentColumn
						},
						attributeName: nodes[i].columnName, 
						attributeType: nodeType, 
						supportedTypes: supportedTypes 
					});
				} else {
					strDuplicate += "<li>"+nodes[i].columnName+"</li>"
				}
			} else {
				$scope.element.attrs.unshift({
					columnData : {
						columnName: nodes[i].columnName,
						tableAlias: preDefTableAlias ? preDefTableAlias : dbUtilities.getTableAlias(colData.name),
						columnType: nodes[i].columnType,
						isParentColumn: nodes[i].isParentColumn
					},
					attributeName: nodes[i].columnName, 
					attributeType: nodeType, 
					supportedTypes: supportedTypes
					});
			}
		}
		if(strDuplicate !== ""){
			popupFactory.showInfoPopup('Warning!', 'Following column names were skipped as they already exist in collection:<br> <ul>'+strDuplicate+'</ul>');
		}
	}
	
	$scope.clearAllAttribs = function(element){
		if($element.parents('.mongo-collection-instance').length != 0){
    		$scope.$emit('request-remove-collection', element);
    	} else {
    		if(element.attrs.length != 0){
    			popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove  all data from collection '+element.attributeName+' ?', function (){
        			$scope.clearEditingData();
            		$scope.clearAttributes();
            		$scope.$apply();
            		$scope.element.sourceTables.length = 0;
    			})
    		}
    		if(element.referencedColumns){
    			element.referencedColumns.length = 0;
    		}
    		
    		
    	}
	}
	
	$scope.clearAttributes = function(){
		if($scope.element.collectionType!='GridFs'){
			$scope.element.attrs.length = 0;
			$scope.element.currentAttribTables.length = 0;
			$scope.element.currentTableRelations.length = 0;
		}else{
			$scope.element.attrs.splice(2, 1);
			$scope.element.attrs = $scope.element.attrs.map(function(att) { 
			    delete att.columnData; 
			    return att; 
			});
			$scope.element.currentAttribTables.length = 0;
			$scope.element.currentTableRelations.length = 0;
			$scope.isGridSubcollectionAdded = false;
		}
	}
	
	$scope.clearEditingData = function(){
		if($scope.currentEditingScope){
			$scope.currentEditingScope.isEditing = false;
		}
		$scope.currentEditingAttrib = null;
		//$scope.attribEditMode = false;
		$scope.clearTableRelationsData();
	}
	
	$scope.addNewAttribute = function(attr, tableName, tableALias){
		$scope.element.attrs.unshift(attr);
		if(attr.columnData.tableAlias){
			if(!attr.columnData.isParentColumn || $element.parents('.mongo-collection-instance').length == 0){ // Either main level collection OR attribute is NOT dragged from parent table : then only make a SourceTables entry for Table
				$scope.addSourceTableIfDoesntExist(tableName, tableALias);
				$scope.pushCurrentTableName(tableName);
			}
			
			/*if($scope.element.currentAttribTables.indexOf(tableName) < 0){
				$scope.element.currentAttribTables.push(tableName);
			}*/
			if($scope.currentRelationJSON){
				$scope.addSourceTable($scope.currentRelationJSON);
				$scope.clearTableRelationsData();
			}
			if($scope.currentparentChildRelationJSON){
				for (var i = 0; i < $scope.currentparentChildRelationJSON.filters.length; i++) {
					$scope.element.filters.push($scope.currentparentChildRelationJSON.filters[i]);
				}
				$scope.clearTableRelationsData();
			}
		}
	}
	
	$scope.clearTableRelationsData = function(){
		$scope.currentRelationJSON = null;
		$scope.currentAttribTable = "";
		$scope.currentparentChildRelationJSON = null;
	}
	
	$scope.removeAttrib = function(attrib){
		popupFactory.showConfirmPopup('Are you sure?', 'Do you want to remove atribute : '+attrib.attributeName+' ?', function (){ 
			var attribPos = $scope.element.attrs.indexOf(attrib);
			if( attribPos > -1){
				$scope.element.attrs.splice(attribPos, 1);
				if(attrib.columnData && attrib.columnData.isParentColumn){
					$scope.$emit('remove-referenced-column', attrib)
				}
				$scope.$apply();
				if($scope.element.attrs.length == 0){
					//$scope.clearAllAttribs($scope.element);
					$scope.clearEditingData();
		    		$scope.clearAttributes();
				}
			} else {
				console.log("Attribute not found");
				//alert('Attrib missing !!!');
			}
		});
	};
	
	$scope.defineOwnFilter = function(){
		var parentWithTables = $scope.upperParentsWithAttributes();
		var parentTables = parentWithTables.tables;
		var availableTables = $scope.element.currentAttribTables;
		var parentRelationMode = false;
		if(parentTables){
			parentRelationMode = true;
			parentTables.forEach(function(element, index, array){
				if(availableTables.indexOf(element) < 0){
					availableTables.push(element);
				}
			})
			//availableTables = availableTables.concat(parentTables)
		}
		
		popupFactory.getTableRelationPopup({
			fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
			collectionName: $scope.element.attributeName,
			scopeTables: availableTables,
			parentRelationMode: parentRelationMode,
			selfFilterMode: true,
			parentTables: parentTables,
			currentFilters: $scope.element.filters,
			colectionName : $scope.element.name, 
			aliasesData: this.element.sourceTables,
			callbackFn: function(jsData){
				$scope.element.filters = (JSON.parse(jsData)).filters;
		}});
	};
	
	$scope.editAttribute = function(attrib){
		$scope.$emit("edit-collection-attribute-popup", attrib, $scope);
	}
	
	$scope.dropTableColumnNameOnEditor = function (ev){
		$scope.dropTableColumnName(ev);
	}
	
    $scope.dropTableColumnName = function (ev) {
        var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
        if(xferData.type === "table"){ // Table is Dragged
        	var tableName = xferData.tableName;
        	var columnsData = [];
        	$rootScope.$emit('request-table-data',{ 
        		tableName: tableName, 
        		callbackFn: function(data){
	        		if(tableName == data.tableName){ // Checking if it is the callback for our call or something else's.
	        			if(!data.tableData.length){
	        				popupFactory.showInfoPopup('Error !!', "The table "+data.tableName+" has no data.");
	        				return;
	        			}
	            		if(xferData.onlyColumns){
	            			$scope.addAllColumns({
	            				name: data.tableName,
	            				nodes: data.tableData
	            			});
	            			$scope.$apply();
	                	} else {
	                		var nodes = data.tableData;
	                		for (var i = 0; i < nodes.length; i++) {
	                			var nodeType = dbUtilities.getDefaultAttributeType(nodes[i].columnType);
	                			var supportedTypes = dbUtilities.getMongoAttrTypes(nodes[i].columnType);
	                			columnsData.push({
	                				columnData : {
	                					columnName: nodes[i].columnName,
	                					tableAlias: dbUtilities.getTableAlias(data.tableName),
	                					columnType: nodes[i].columnType
	                				},
	                				attributeName: nodes[i].columnName,
	                				attributeType: nodeType,
	                				supportedTypes: supportedTypes
                				});
	                		};
	                		if($scope.element.currentAttribTables.length != 0){ // Table attributes present already
	                			//if($scope.element.currentAttribTables.length == 1 && $scope.element.currentAttribTables[0] == xferData.tableName){ // Only Same table attributes are there in collection TODO : Check if its really needed t obe only one table in current table
                				if($scope.element.currentAttribTables.length && $scope.element.currentAttribTables.indexOf(xferData.tableName) > -1){ // Only Same table attributes are there in collection
	                				
	                				// As we are creating subcollection with name of dragged tableName we need to check if same named subcollection already exists
	                				for (var i = 0; i < $scope.element.attrs.length; i++) {
		                				if($scope.element.attrs[i].attributeName === tableName){
		                					popupFactory.showInfoPopup('Error!', 'Subcollection with name '+tableName+' already exist. Create a subcollection manually and add columns to it.');
		                					return;
		                				}
	                				}
	                				
	                				$scope.checkSelfJooinedTableCounter();
	                				
	                				
	                				$scope.confirmSameParentTableRelation({tableName: tableName, existingTableAliases: $scope.element.sourceTables, tableDragged: true}, function(jsData){
	                					
	                					var CollectionBlock = {
	            							attributeName: tableName,
		    	            				attributeType: "AUTO", 
		    	            				attrs: columnsData,
		    	            				currentAttribTables: [tableName]
		    	        				};
	        	                    	if(jsData){
	        	                    		var relJSON = JSON.parse(jsData);
	        	                    		CollectionBlock.filters = relJSON.filters;
											CollectionBlock.attrs.forEach(function(item, index){
												item.columnData.tableAlias = dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter; 
											});
											dbUtilities.setSameNameFromAlias(tableName, dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter);
											CollectionBlock.sourceTables = [{
		    	            					"tableName": tableName,
		    	            		        	"tableAlias": dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter,
		    	            				}]
	        	                    	} else {
	        	                    		CollectionBlock.sourceTables = [];
	        	                    		$scope.processParentTableRelatedNodes(columnsData);
	        	                    	}
	        	                    	$scope.element.attrs.push(CollectionBlock);
	        	                    	//$scope.$apply();
	        	                    	$timeout(function () { // Some cases fail to update UI
	        	        					$scope.$apply();
	        	        				}, 0, false);
	                				});
	                			} else {
	                				popupFactory.getTableRelationPopup({
		            					tableName: xferData.tableName, 
		            					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
		            					parentRelationMode: true,
		            					scopeTables: $scope.element.currentAttribTables,
		            					needRefresh: true,
		            					callbackFn: function(jsData){
		            						var relJSON = JSON.parse(jsData);
		            						var CollectionBlock = {
		            							attributeName: tableName,
	    	    	            				attributeType: "AUTO", 
	    	    	            				attrs: columnsData,
	    	    	            				sourceTables: [{
	    	    	            					"tableName": relJSON.mainTableName,
	    	    	            		        	"tableAlias": dbUtilities.getTableAlias(relJSON.mainTableName),
	    	    	            				}],
	    	    	            				currentAttribTables: [tableName],
	    	    	            				filters: relJSON.filters
	    	    	        				};
		        	                    	$scope.element.attrs.push(CollectionBlock);
		            					}
		            				});
	                			}
	                			
	                		} else {
	                			/* NOTE : This might should restrict adding subCollection if there is no attribute present in the Parent collection to relate */
	                			// LOOK FOR UPPER PARENTS IF THEY HAVE ANY ATTRINUTES TO RELATE
	                			var upperParentWithTables = $scope.upperParentsWithAttributes();
	                			var upperParentTables = upperParentWithTables.tables;
	                			if(upperParentTables){
	                				//if(upperParentTables.length == 1 && (upperParentTables.indexOf(xferData.tableName) >= 0)){ // Parent has same table and its only table parent has TODO : Check if parent really needed to have only 1 table
                					if(upperParentTables.length && (upperParentTables.indexOf(xferData.tableName) >= 0)){ // Parent has same table in its table list
	                					// No need to show tableRelation just add parentCollection attribute as parent has (Only) same table 
                						$scope.checkSelfJooinedTableCounter();
                						
                						var CollectionBlock = {
    	            							attributeName: tableName,
        	    	            				attributeType: "AUTO", 
        	    	            				attrs: columnsData,
        	    	            				sourceTables: [/*{
        	    	            					"tableName": tableName,
        	    	            		        	"tableAlias": dbUtilities.getTableAlias(tableName),
        	    	            				}*/],
        	    	            				currentAttribTables: [] // No Need to add attribTable as attribute is dragged from Parent
    	    	        				};
                						
                						$scope.confirmSameParentTableRelation({tableName: tableName, existingTableAliases: $scope.element.sourceTables, tableDragged: true}, function(jsData){
                							if(jsData){
                								columnsData.forEach(function(item, index){
    												item.columnData.tableAlias = dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter; 
    											});
                								dbUtilities.setSameNameFromAlias(tableName, dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter);
                								CollectionBlock.sourceTables = [{
					                                "tableName": tableName,
        	    	            		        	"tableAlias": dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter
                								}];
                								CollectionBlock.currentAttribTables.push(tableName);
                								var relJSON = JSON.parse(jsData);
    	        	                    		CollectionBlock.filters = relJSON.filters;
                							} else {
                								columnsData.forEach(function(element, index, array){
        	                						upperParentWithTables.collection.addRefAttribute($.extend({},element));
        	                					});
                								$scope.processParentTableRelatedNodes(columnsData);
                							}
    	        	                    	$scope.element.attrs.push(CollectionBlock);
                						})
	                				} else {
	                					popupFactory.getTableRelationPopup({
			            					tableName: xferData.tableName, 
			            					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
			            					parentRelationMode: true,
			            					scopeTables: upperParentTables,
			            					needRefresh: true,
			            					callbackFn: function(jsData){
			            						var relJSON = JSON.parse(jsData);
			            						var CollectionBlock = {
			            							attributeName: tableName,
		    	    	            				attributeType: "AUTO", 
		    	    	            				attrs: columnsData,
		    	    	            				sourceTables: [{
		    	    	            					"tableName": relJSON.mainTableName,
		    	    	            		        	"tableAlias": dbUtilities.getTableAlias(relJSON.mainTableName),
		    	    	            				}],
		    	    	            				currentAttribTables: [tableName],
		    	    	            				filters: relJSON.filters
		    	    	        				};
			        	                    	$scope.element.attrs.push(CollectionBlock);
			            					}
			            				});
	                				}
	                				//if(upperParentTables.length == 1 && )
	                				
	                				
	                				// Relate to multiLevel parent and save
	                			} else {
	                				popupFactory.showInfoPopup('Error !!', "No parent with attribute found. Please add attribute to atleast one parent collection.");
	                			}
	                			
	                			/*var CollectionBlock = {
	                				attributeName: tableName,
    	            				attributeType: "COLLECTION", 
    	            				attrs: columnsData,
    	            				sourceTables: [{
    	            					"tableName": tableName,
    	            		        	"tableAlias": dbUtilities.getTableAlias(tableName),
    	            				}],
    	            				currentAttribTables: [tableName]
    	        				};
	                			//$scope.addSourceTableIfDoesntExist(tableName);
    	                    	$scope.element.attrs.push(CollectionBlock);
    	                    	$scope.$apply();*/
	                		}
            				
	                	}
	        		}
	        	}
        	});
        }else { // Single column is dragged
        	var attribTypes = dbUtilities.getMongoAttrTypes(xferData.type);
            if($scope.checkCurrentTableName(xferData.tableName)){
            	var collectionType = $scope.collections[0].collectionType;
            	if($scope.isParentRelationRequired(xferData.tableName)){
            		var upperParentWithTables = $scope.upperParentsWithAttributes();
            		var upperParentTables = upperParentWithTables.tables;
        			if(upperParentTables){
        				if(upperParentTables.length > 0 && (upperParentTables.indexOf(xferData.tableName) >= 0)){
        					// No need to show tableRelation just add parentCollection attribute as parent has (Only) same table 
        					// TODO : Add confirmation if user wants to define relationship with the parent table (which is same as its own table ) HERE
        					
        					$scope.checkSelfJooinedTableCounter();
        					
        					$scope.confirmSameParentTableRelation({tableName : xferData.tableName, existingTableAliases: $scope.element.sourceTables}, function(jsData){
        						if(jsData){
        							var relJSON = JSON.parse(jsData);
        							if(relJSON.filters.length){
        								$scope.currentparentChildRelationJSON = JSON.parse(jsData);
        							}
        							xferData.preDefAlias = dbUtilities.getTableAlias(xferData.tableName)+"_E"+$scope.$id+$scope.element.extTableCounter;
        							dbUtilities.setSameNameFromAlias(xferData.tableName, dbUtilities.getTableAlias(xferData.tableName)+"_E"+$scope.$id+$scope.element.extTableCounter);
        							xferData.isParentColumn = false;
        						} else {
        							upperParentWithTables.collection.addRefAttribute({
                						"columnName": xferData.label,
                		                "tableAlias": dbUtilities.getTableAlias(xferData.tableName),
                		                "columnType": dbUtilities.getMongoAttrTypes(xferData.type)[0]
                					}); // TODO : If user cancels add attribute. This needs to be undone.
        							xferData.isParentColumn = true;
        						}
        						
            					
            					$scope.$emit("add-collection-attribute-popup", xferData, $scope);
        					})
        					
        				} else {
        					popupFactory.getTableRelationPopup({
            					tableName: xferData.tableName, 
            					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
            					parentRelationMode: true,
            					scopeTables: upperParentTables,
            					needRefresh: true,
            					callbackFn: function(jsData){
            						upperParentWithTables.collection.addRefAttribute({
                						"columnName": xferData.label,
                		                "tableAlias": dbUtilities.getTableAlias(xferData.tableName),
                		                "columnType": dbUtilities.getMongoAttrTypes(xferData.type)[0]
                					}); 
            						//xferData.isParentColumn = true;
            						$scope.currentparentChildRelationJSON = JSON.parse(jsData);
            						$scope.$emit("add-collection-attribute-popup", xferData, $scope);
            						//$scope.pushCurrentTableName(xferData.tableName);
            					}
            				});
        				}
        			} else {
        				popupFactory.showInfoPopup('Error !!', "Please map column to atleast one parent attribute.")
        			}
    			} else {
    				$scope.$emit("add-collection-attribute-popup", xferData, $scope);
    			}
            } 
            else {
            	popupFactory.getTableRelationPopup({
            		tableName: xferData.tableName, 
    				fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
    				scopeTables: $scope.element.currentAttribTables,
    				needRefresh: true, // Drag and Drop doesn't fire digest cycle of the popup
    				callbackFn: function(jsRelData){
    					$scope.element.currentTableRelations.push(JSON.parse(jsRelData));
    					$scope.currentRelationJSON = JSON.parse(jsRelData);
    					$scope.$emit("add-collection-attribute-popup", xferData, $scope);
    					//$scope.pushCurrentTableName(xferData.tableName)
    				}
            	});
            }
        }
    };
    
    $scope.checkSelfJooinedTableCounter = function(){
    	if(!$scope.element.extTableCounter){
			$scope.element.extTableCounter = 0
		} else {
			$scope.element.extTableCounter++;
		}
    }
    
    $scope.confirmSameParentTableRelation = function(tableData, callbackFnRef){
    	var tableName = tableData.tableName;
    	popupFactory.showConfirmPopup('Same parent table found!!', 'Do you want to define/use relation with same parent table : '+tableName+' ? (optional). If not, same values will be copied in the subcollection.', function (){
    		
    		if($scope.element.filters && $scope.element.filters.length && !tableData.tableDragged){ // Check if existing SELF FILTER is there and its for the same table [ Only for add all columns, not tableDrag]
    			for (var i = 0; i < $scope.element.filters.length; i++) {
					var filter = $scope.element.filters[i];
					if(filter.rightHandExpression.expressionType == "column" && filter.leftHandExpression.expressionType == "column"){
						if(filter.rightHandExpression.columnData.tableName == filter.leftHandExpression.columnData.tableName && filter.rightHandExpression.columnData.tableAlias != filter.leftHandExpression.columnData.tableAlias){
							callbackFnRef("{\"filters\":[]}");
							$scope.element.extTableCounter--;
							return;
						}
					}
				}
    		}
    		
    		popupFactory.getTableRelationPopup({
				tableName: tableName, 
				fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
				parentRelationMode: true,
				sameParentTableRelationMode: true,
				scopeTables: [tableName],
				needRefresh: true,
				callbackFn: function(jsData){
					var relJSON = JSON.parse(jsData);
            		//CollectionBlock.filters = relJSON.filters;
					relJSON.filters.forEach(function(filter, index){
						if(filter.rightHandExpression.expressionType == "column"){
							filter.rightHandExpression.columnData.tableAlias = dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter;
							dbUtilities.setSameNameFromAlias(tableName, dbUtilities.getTableAlias(tableName)+"_E"+$scope.$id+$scope.element.extTableCounter);
							filter.rightHandExpression.columnData.isParentColumn = false;
						};
						if(index == 0){
							filter.leftHandExpression.columnData.isParentColumn = true;
						}
						if(tableData.existingTableAliases){
							/* Table relations editor returns aliases using dbUtilities.getTableAlias function which is unaware of repeated tables. Need to fix this. */
							for (var j = 0; j < tableData.existingTableAliases.length; j++) {
								if(filter.leftHandExpression.columnData.tableAlias == dbUtilities.getTableAlias(tableData.existingTableAliases[j].tableName)){
									filter.leftHandExpression.columnData.tableAlias = tableData.existingTableAliases[j].tableAlias;
								}
							}
						}
					});
					var newJsonData = JSON.stringify(relJSON);
					callbackFnRef(newJsonData);
				}
			});
		}, true, callbackFnRef)
    }
    
    $scope.getTableReference = function(sourceTables, tableName){
    	var tableIndexes = $.grep(sourceTables, function(e){ return e.tableName == tableName; });
    	if(tableIndexes.length){
    		return tableIndexes;
    	} else {
    		//return $scope.getTableReference(sourceTables[0].joinedTables, tableName);
    		for (var i = 0; i < sourceTables.length; i++) {
				var array_element = sourceTables[i].joinedTables;
				if(array_element){
					return $scope.getTableReference(array_element, tableName);
				}
			}
    		return [];
    	}
    }
    
    /* Adds isParentColumn = true for all the columns. This must be called when parent has same columns that are dragged into child collection and no SELF JOIN is defined */
    $scope.processParentTableRelatedNodes = function(colData){
    	for (var i = 0; i < colData.length; i++) {
    		if(colData[i].columnName){
    			colData[i].isParentColumn = true;
    		}
    		if(colData[i].columnData){
    			colData[i].columnData.isParentColumn = true;
    		}
		}
    }
    
    $scope.addSourceTable = function(relJson, scopeSourceRef){
    	// Add  this relation to the joinedTables of relJson.tableName    	
    	var sourceTableMain = scopeSourceRef ? scopeSourceRef : $scope.element.sourceTables;
    	var tableIndexes = $scope.getTableReference(sourceTableMain, relJson.tableName)
    	
    	if(tableIndexes.length == 0){
    		console.log('Cannot add Joinedtable >> as : '+relJson.tableName+' not found !!!!');
    		return;
    	}
    	if(!tableIndexes[0].joinedTables){
    		tableIndexes[0].joinedTables = [];
    	}
    	
    	var tableExistInJoinedTables = $.grep(tableIndexes[0].joinedTables, function(e){ return e.tableName == relJson.mainTableName});
    	
    	/* NOTE: If its current editing relations just change the filters chunk and  join type */
    	if(tableExistInJoinedTables.length != 0){
    		tableExistInJoinedTables[0].filters = relJson.filters;
    		tableExistInJoinedTables[0].joinType = relJson.joinType;
    	} else {
    		tableIndexes[0].joinedTables.push({
        		"tableName": relJson.mainTableName,
            	"tableAlias": dbUtilities.getTableAlias(relJson.mainTableName),
            	"joinType": relJson.joinType,
            	"filters": relJson.filters
        	})
    	}
    };
    
    $scope.isParentRelationRequired = function(tableName){
    	if($scope.element.currentAttribTables.length == 0 || $scope.allParentAttributes()){
    		// TODO Find a better way to find parent
        	if($element.parents('.mongo-collection-instance').length != 0){
        		return true;
        	}
    	}
    	if($scope.element.currentAttribTables.length != 0 && $scope.isParentAttribute(tableName)){
    		return true;
    	}
    	return false;
    }
    
    $scope.upperParentsWithAttributes = function(){
    	var parentCollections = $element.parents('.mongo-collection-instance');
    	for (var i = 0; i < parentCollections.length; i++) {
			var elem = angular.element(parentCollections[i]).scope().element;
			if(elem.currentAttribTables && elem.currentAttribTables.length > 0){
				return {
					tables : elem.currentAttribTables,
					collection: angular.element(parentCollections[i]).scope(),
					sourceTables: elem.sourceTables
				}
			}
		}
    	return false;
    }
    
    $scope.isParentAttribute = function(tableName){
    	var parentCollections = $element.parents('.mongo-collection-instance');
    	for (var i = 0; i < parentCollections.length; i++) {
			var elem = angular.element(parentCollections[i]).scope().element;
			if(elem.currentAttribTables && elem.currentAttribTables.length > 0){
				if(elem.currentAttribTables.indexOf(tableName) >= 0){
					return true;
				}
			}
		}
    	return false;
    }
    
    $scope.getParentTables = function(scope){
    	
    	// TODO Find a better way to find parent scope
    	return $scope.$parent.$parent.$parent.element.currentAttribTables;
    }
    
    $scope.addSourceTableIfDoesntExist = function(tableName, tableALias){
    	var tableIndexes = $scope.getTableReference($scope.element.sourceTables, tableName);
		if(tableIndexes.length == 0 && $scope.currentRelationJSON == null){
			$scope.element.sourceTables.push({
				tableName: tableName,
				tableAlias: tableALias ? tableALias : dbUtilities.getTableAlias(tableName)
			});
		}
    }
    
    $scope.checkCurrentTableName = function(tableName){
    	if(!$scope.element.currentAttribTables.length || !$scope.element.attrs.length){
    		var isAttrWithColumn = true; 
    		for (var i = 0; i < $scope.element.attrs.length; i++) {
    			if($scope.element.attrs[i].columnData && $scope.element.attrs[i].columnData.columnName){
    				if($scope.element.attrs[i].columnData && !$scope.element.attrs[i].columnData.isParentColumn){
    					return false;
    				}
    			}
			}
    		return isAttrWithColumn;
    		/*if(isAttrWithColumn){
    			return true;
    		} else {
    			return false;
    		}*/
			//$scope.element.currentAttribTables.push(tableName);
    		
    	} else if($scope.isParentAttribute(tableName)){
    		return true;
    	} else if($scope.element.currentAttribTables.indexOf(tableName) == -1){
    		return false;
    	} else if($scope.element.attrs.length && $scope.allParentAttributes()){ // if tables are there but all are parent attributes !!
    		return true;
    	}else {
    		return true;
    	}
    };
    
    $scope.allParentAttributes = function(){
    	var allParentAttributes = true;
		for (var i = 0; i < $scope.element.attrs.length; i++) {
			if($scope.element.attrs[i].columnData && !$scope.element.attrs[i].columnData.isParentColumn){
				allParentAttributes = false;
			}
		};
		return allParentAttributes;
    }
    
    $scope.pushCurrentTableName = function(tableName){
    	if(!$scope.element.currentAttribTables){
    		$scope.element.currentAttribTables = [];
    	}
    	if(tableName){
    		if($scope.element.currentAttribTables.indexOf(tableName) < 0){
        		$scope.element.currentAttribTables.push(tableName);
        	}
    	}
    }
    
    $scope.isValidAttributeName = function(atribName){
    	var validData = {
			isValid : true,
			errMsg: ''
    	}
    	for (var i = 0; i < $scope.element.attrs.length; i++) {
			if($scope.element.attrs[i].attributeName === atribName){
				validData.isValid = false;
				validData.errMsg = 'Attribute name already exist.';
			}
		}
    	return validData;
    }
   
    /*$scope.dropGridFSAttrib = function(ev){
		console.log("dbCollectionInstanceController call");
		var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
		if(!xferData.dragType || xferData.dragType != "column"){
			popupFactory.showInfoPopup('Access Denied!!', 'Only collection attributes are allowed to drag !!.', false);
			return;
		}
		for(var i =0; i< $scope.element.attrs.length; i++){
			var elem =  $scope.element.attrs[i];
			var attribName = elem.attributeName;
			var attribType = elem.attributeType;
			if(attribName == ev.originalEvent.toElement.innerHTML){
				if(attribType != dbUtilities.getMongoAttrTypes(xferData.type)){
					popupFactory.showInfoPopup('Access Denied!!','Only column of type '+attribType+ ' are allowed to drag for attribute '+attribName+ '!!.', false);
					return;
				}else{
					$scope.element.attrs[i].columnData = {
							columnName: xferData.label,
							tableName: xferData.tableName,
							columnType : xferData.type,
							tableAlias : dbUtilities.getTableAlias(xferData.tableName),
							isIdentifier : (xferData.isIdentifier == "true") ? true : false
					};
					if(attribName == 'INPUT_STREAM'){
						//add source table only if attribute is Input_stream
						$scope.addSourceTableIfDoesntExist(xferData.tableName);
					}
					// add table name as parent currentAttribTables
					$scope.pushCurrentTableName(xferData.tableName);
					$scope.editAttribute($scope.element.attrs[i]);
					$scope.$apply();
				}
			}
		}
	}*/
    
    $scope.dropGridFSAttrib = function(ev){
    	var xferData = JSON.parse(ev.originalEvent.dataTransfer.getData("text"));
    	console.log("dropGridFSAttrib call with xferData :: "+xferData);
    	if(xferData.type == "table"){
			// if table is dragged
    		
		}else{
			// if column is dragged
			var attribTypes = dbUtilities.getMongoAttrTypes(xferData.type);
			if($scope.checkCurrentTableName(xferData.tableName)){
				if($scope.isParentRelationRequired(xferData.tableName)){
					var upperParentWithTables = $scope.upperParentsWithAttributes();
            		var upperParentTables = upperParentWithTables.tables;
            		console.log('xferData.tableName '+xferData.tableName);
            		console.log('upperParentWithTables '+upperParentWithTables +'upperParentTables '+upperParentTables);
            		if(upperParentTables){
        				if(upperParentTables.length > 0 && (upperParentTables.indexOf(xferData.tableName) >= 0)){
        					// No need to show tableRelation just add parentCollection attribute as parent has (Only) same table 
        					// TODO : Add confirmation if user wants to define relationship with the parent table (which is same as its own table ) HERE
        					
        					$scope.checkSelfJooinedTableCounter();
        					
        					$scope.confirmSameParentTableRelation({tableName : xferData.tableName, existingTableAliases: $scope.element.sourceTables}, function(jsData){
        						if(jsData){
        							var relJSON = JSON.parse(jsData);
        							if(relJSON.filters.length){
        								$scope.currentparentChildRelationJSON = JSON.parse(jsData);
        							}
        							xferData.preDefAlias = dbUtilities.getTableAlias(xferData.tableName)+"_E"+$scope.$id+$scope.element.extTableCounter;
        							dbUtilities.setSameNameFromAlias(xferData.tableName, dbUtilities.getTableAlias(xferData.tableName)+"_E"+$scope.$id+$scope.element.extTableCounter);
        							xferData.isParentColumn = false;
        						} else {
        							upperParentWithTables.collection.addRefAttribute({
                						"columnName": xferData.label,
                		                "tableAlias": dbUtilities.getTableAlias(xferData.tableName),
                		                "columnType": dbUtilities.getMongoAttrTypes(xferData.type)[0]
                					}); // TODO : If user cancels add attribute. This needs to be undone.
        							xferData.isParentColumn = true;
        						}
            					$scope.$emit("add-collection-attribute-popup", xferData, $scope);
        					})
        				} else {
        					console.log('data is dragged on columns');
        					var currentfoundAttr = null;
        					var draggedAttr = ev.originalEvent.toElement.innerHTML;
        					for(var i =0; i< $scope.element.attrs.length; i++){
        						var elem =  $scope.element.attrs[i];
        						var attribName = elem.attributeName;
        						var attribType = elem.attributeType;
        						if(attribName == draggedAttr && (attribName == 'FILE_NAME' || attribName == 'INPUT_STREAM')){
        							currentfoundAttr = elem;
        						}
        					}
        					if(currentfoundAttr !=null){
        						if(currentfoundAttr.attributeType != dbUtilities.getMongoAttrTypes(xferData.type)){
    								popupFactory.showInfoPopup('Access Denied!!','Only column of type '+currentfoundAttr.attributeType+ ' are allowed to drag for attribute '+currentfoundAttr.attributeName+ '!!.', false);
    								return;
    							}
    								popupFactory.getTableRelationPopup({
    	            					tableName: xferData.tableName, 
    	            					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
    	            					parentRelationMode: true,
    	            					scopeTables: upperParentTables,
    	            					needRefresh: true,
    	            					callbackFn: function(jsData){
    	            						upperParentWithTables.collection.addRefAttribute({
    	                						"columnName": xferData.label,
    	                		                "tableAlias": dbUtilities.getTableAlias(xferData.tableName),
    	                		                "columnType": dbUtilities.getMongoAttrTypes(xferData.type)[0]
    	                					}); 
    	            						currentfoundAttr.columnData = {
            										columnName: xferData.label,
            										tableName: xferData.tableName,
            										columnType : xferData.type,
            										tableAlias : dbUtilities.getTableAlias(xferData.tableName),
            										isIdentifier : (xferData.isIdentifier == "true") ? true : false
            								};
    	            						$scope.currentparentChildRelationJSON = JSON.parse(jsData);
    	            						if(draggedAttr == 'INPUT_STREAM'){
    	            							//add source table only if attribute is Input_stream
    	            							$scope.addSourceTableIfDoesntExist(xferData.tableName);
    	            						}
    	            						// add table name as parent currentAttribTables
    	            						$scope.$emit("edit-collection-attribute-popup", currentfoundAttr, $scope);
    	            						//$scope.$apply();
    	            					}
    	            				});
        					} else{
        							// data is dragged on META_ATTRIBUTES
        							console.log('data is dragged on META_ATTRIBUTES');
        							popupFactory.getTableRelationPopup({
                    					tableName: xferData.tableName, 
                    					fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
                    					parentRelationMode: true,
                    					scopeTables: upperParentTables,
                    					needRefresh: true,
                    					callbackFn: function(jsData){
                    						upperParentWithTables.collection.addRefAttribute({
                        						"columnName": xferData.label,
                        		                "tableAlias": dbUtilities.getTableAlias(xferData.tableName),
                        		                "columnType": dbUtilities.getMongoAttrTypes(xferData.type)[0]
                        					}); 
                    						//xferData.isParentColumn = true;
                    						$scope.currentparentChildRelationJSON = JSON.parse(jsData);
                    						$scope.$emit("add-collection-attribute-popup", xferData, $scope);
                    						//$scope.pushCurrentTableName(xferData.tableName);
                    					}
                    				});
        					}
        					
        				}
        			} else{
        				console.log('No Column is mapped to parent attributes');
        				popupFactory.showInfoPopup('Error !!', "Please map column to atleast one parent attribute.")
        			}
				} else {
					console.log('if columns dragged are of same table as parent');
    				var draggedAttr = ev.originalEvent.toElement.innerHTML;
					for(var i =0; i< $scope.element.attrs.length; i++){
						var elem =  $scope.element.attrs[i];
						var attribName = elem.attributeName;
						var attribType = elem.attributeType;
						if(attribName == draggedAttr && (attribName == 'FILE_NAME' || attribName == 'INPUT_STREAM')){
							currentfoundAttr = elem;
						}
					}
					if(currentfoundAttr !=null){
						if(currentfoundAttr.attributeType != dbUtilities.getMongoAttrTypes(xferData.type)){
							popupFactory.showInfoPopup('Access Denied!!','Only column of type '+currentfoundAttr.attributeType+ ' are allowed to drag for attribute '+currentfoundAttr.attributeName+ '!!.', false);
							return;
						}
						currentfoundAttr.columnData = {
	        						columnName: xferData.label,
	        						tableName: xferData.tableName,
	        						columnType : xferData.type,
	        						tableAlias : dbUtilities.getTableAlias(xferData.tableName),
	        						isIdentifier : (xferData.isIdentifier == "true") ? true : false
	        				};
		            		if(draggedAttr == 'INPUT_STREAM'){
		            			//add source table only if attribute is Input_stream
		            			$scope.addSourceTableIfDoesntExist(xferData.tableName);
		            		}
		            		$scope.pushCurrentTableName(xferData.tableName);
		            		$scope.$emit("edit-collection-attribute-popup", currentfoundAttr, $scope);
		            		$scope.$apply();
					} else{
						// data is dragged on META_ATTRIBUTES
						console.log('data is dragged on META_ATTRIBUTES');
						$scope.$emit("add-collection-attribute-popup", xferData, $scope);
					  }
					
    			}
			}else{
				var draggedAttr = ev.originalEvent.toElement.innerHTML;
				var currentfoundAttr = null;
				for(var i =0; i< $scope.element.attrs.length; i++){
					var elem =  $scope.element.attrs[i];
					var attribName = elem.attributeName;
					var attribType = elem.attributeType;
					if(attribName == draggedAttr && (attribName == 'FILE_NAME' || attribName == 'INPUT_STREAM')){
						 currentfoundAttr = elem;
					}
				}
				console.log('currentfoundAttr '+currentfoundAttr);
				if(currentfoundAttr !=null){
					if(currentfoundAttr.attributeType != dbUtilities.getMongoAttrTypes(xferData.type)){
						popupFactory.showInfoPopup('Access Denied!!','Only column of type '+currentfoundAttr.attributeType+ ' are allowed to drag for attribute '+currentfoundAttr.attributeName+ '!!.', false);
						return;
					}
							popupFactory.getTableRelationPopup({
				            tableName: xferData.tableName, 
				    				fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
				    				scopeTables: $scope.element.currentAttribTables,
				    				needRefresh: true, // Drag and Drop doesn't fire digest cycle of the popup
				    				callbackFn: function(jsRelData){
				    					console.log('jsRelData: '+JSON.parse(jsRelData));
				    					$scope.element.currentTableRelations.push(JSON.parse(jsRelData));
				    					$scope.currentRelationJSON = JSON.parse(jsRelData);
				    					currentfoundAttr.columnData = {
	    										columnName: xferData.label,
	    										tableName: xferData.tableName,
	    										columnType : xferData.type,
	    										tableAlias : dbUtilities.getTableAlias(xferData.tableName),
	    										isIdentifier : (xferData.isIdentifier == "true") ? true : false
	    								};
	            						//$scope.currentparentChildRelationJSON = JSON.parse(jsRelData);
	            						if(draggedAttr == 'INPUT_STREAM'){
	            							//add source table only if attribute is Input_stream
	            							$scope.addSourceTableIfDoesntExist(xferData.tableName);
	            						}
	            						$scope.$emit("edit-collection-attribute-popup", currentfoundAttr, $scope);
	            						//$scope.$apply();
				    				}
				            	});
				}
				else{
							// data dragged on meta attributes
							console.log('data dragged on meta attributes');
			            	popupFactory.getTableRelationPopup({
			            		tableName: xferData.tableName, 
			    				fetchedTablesData: $scope.mainScope.getCurrentFetchedData(),
			    				scopeTables: $scope.element.currentAttribTables,
			    				needRefresh: true, // Drag and Drop doesn't fire digest cycle of the popup
			    				callbackFn: function(jsRelData){
			    					$scope.element.currentTableRelations.push(JSON.parse(jsRelData));
			    					$scope.currentRelationJSON = JSON.parse(jsRelData);
			    					$scope.$emit("add-collection-attribute-popup", xferData, $scope);
			    					//$scope.pushCurrentTableName(xferData.tableName)
			    				}
			            	});
						}
					/*}
				}*/
			}
		}
    }
    
    $scope.editGridAttribute = function(attr, tableName, tableALias){
		if(attr.columnData.tableAlias){
			if(!attr.columnData.isParentColumn || $element.parents('.mongo-collection-instance').length == 0){ // Either main level collection OR attribute is NOT dragged from parent table : then only make a SourceTables entry for Table
				$scope.addSourceTableIfDoesntExist(tableName, tableALias);
				$scope.pushCurrentTableName(tableName);
			}
			
			/*if($scope.element.currentAttribTables.indexOf(tableName) < 0){
				$scope.element.currentAttribTables.push(tableName);
			}*/
			if($scope.currentRelationJSON){
				$scope.addSourceTable($scope.currentRelationJSON);
				$scope.clearTableRelationsData();
			}
			if($scope.currentparentChildRelationJSON){
				for (var i = 0; i < $scope.currentparentChildRelationJSON.filters.length; i++) {
					$scope.element.filters.push($scope.currentparentChildRelationJSON.filters[i]);
				}
				$scope.clearTableRelationsData();
			}
		}
	}
    
});

DBMigratorApp.controller('dbMongoAttributeNode', function ($scope, $interval, popupFactory, $rootScope, $controller){
	angular.extend(this, $controller('expandableNode', {$scope: $scope}));
	$scope.editAttrib = function(attrib){
		$scope.isEditing = true;
		$scope.editAttribute(attrib);
	}
});

DBMigratorApp.controller('selectIdentifierController', function ($scope, $interval, popupFactory, $rootScope, $controller){
	$scope.selectIdentifier = function(attr){
		attr.isIdentifier = !attr.isIdentifier;
	}
});