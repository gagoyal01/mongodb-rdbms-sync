DBMigratorApp.factory('popupFactory', function (){
	var nInfoMode = true;
	var $DBMigratorPopup = $('#DBMigratorPopup');
	var $InfoPopupOkBtn = $('#infoPopupOkBtn');
	var $InfoPopupCancelBtn = $('#infoPopupCancelBtn');
	var $PopupInputField = $('#popupInputField');
	var $InputPopupSubmitBtn = $('#inputPopupSubmitBtn');
	var $DBMigratorSmallPopup = $('#DBMigratorSmallPopup');
	var $DBMigratorPopupErrorLabel = $('#DBMigratorPopup .input-error-label');
	var $DBMigratorPopupMessage =  $('#DBMigratorPopup .message-section');
	var $DBMigratorPopupTitle =  $('#DBMigratorPopup .modal-title');
	var $DBMigratorNameDescriptionPopup = $('#DBMigratorNameDescriptionPopup');
	var $ProgressBar = $('#DBMigratorPopup .progress');
	var $nameDescPopupSubmitBtn = $('#nameDescPopupSubmitBtn');
	
	var calcDomElements = function(){
		$DBMigratorPopup = $('#DBMigratorPopup');
		$InfoPopupOkBtn = $('#infoPopupOkBtn');
		$InfoPopupCancelBtn = $('#infoPopupCancelBtn');
		$PopupInputField = $('#popupInputField');
		$InputPopupSubmitBtn = $('#inputPopupSubmitBtn');
		$DBMigratorSmallPopup = $('#DBMigratorSmallPopup');
		$DBMigratorPopupErrorLabel = $('#DBMigratorPopup .input-error-label');
		$DBMigratorPopupMessage =  $('#DBMigratorPopup .message-section');
		$DBMigratorPopupTitle =  $('#DBMigratorPopup .modal-title');
		$DBMigratorNameDescriptionPopup = $('#DBMigratorNameDescriptionPopup');
		$ProgressBar = $('#DBMigratorPopup .progress');
		$nameDescPopupSubmitBtn = $('#nameDescPopupSubmitBtn');
	};
	return {
		
		initPopupLogic : function (){
			if($DBMigratorPopup.length == 0){
				calcDomElements();
			}
		},
		
		showConfirmPopup: function(title, content, callbaclkFn, yesNoType, fnCallbackForNo){
			this.showInfoPopup(title, content);
			$InfoPopupCancelBtn.show();
			if(yesNoType){
				$InfoPopupOkBtn.text("Yes");
				$InfoPopupCancelBtn.text("No");
			} else {
				$InfoPopupOkBtn.text("OK");
				$InfoPopupCancelBtn.text("Cancel");
			}
			$DBMigratorPopup.off('hidden.bs.modal').on('hidden.bs.modal', function () {
				$InfoPopupCancelBtn.hide();
			});
			$InfoPopupOkBtn.off('click').on('click', function () {
				if(callbaclkFn){
					$InfoPopupOkBtn.off('click');
					callbaclkFn();
				}
			});
			if(fnCallbackForNo){
				$InfoPopupCancelBtn.off('click').on('click', function () {
					$InfoPopupOkBtn.off('click');
					fnCallbackForNo();
				});
			}
			
		},
		
		showInfoPopup: function (title, content, showProgress, fnCallback){
			$DBMigratorPopupErrorLabel.text("").hide();// Remove previous errors
			if($DBMigratorPopup.length == 0){
				/* TODO : Find a better way to calculate DOM elements on load of application. This is just a workaround as this pop up is called first  */
				calcDomElements();
			}
			//$('#DBMigratorPopup .progress-section').hide();
			$InfoPopupOkBtn.text("OK");
			this.updateInfoPopup(title, content);
			$InfoPopupCancelBtn.hide();
			this.setInfoPopupMode(true);
			if(showProgress){
				$ProgressBar.show();
			} else {
				$ProgressBar.hide();
			}
			$DBMigratorPopup.modal('show');
			$InfoPopupOkBtn.off('click').on('click', function () {
				if(fnCallback){
					$InfoPopupOkBtn.off('click');
					fnCallback();
				}
			});
		},
		
		setInfoPopupMode: function(bInfo){
			this.nInfoMode = bInfo;
			if(this.nInfoMode){
				$PopupInputField.hide();
				$InputPopupSubmitBtn.hide();
				$InfoPopupOkBtn.show();
			} else {
				$InputPopupSubmitBtn.show();
				$InfoPopupOkBtn.hide();
				$PopupInputField.show();
				$ProgressBar.hide();
			}
		},
		
		updateInfoPopup: function(title, content){
			$DBMigratorPopupMessage.empty();
			$DBMigratorPopupTitle.empty();
			if(title){
				$DBMigratorPopupTitle.html(title);
			}
			if(content){
				$DBMigratorPopupMessage.html(content);
			}
		},
		
		getTitleDescriptionPopup: function(strData, fnCallback, title, description){
			var $nameField = $('#nameDescPopupNameField');
			var $descField = $('#nameDescPopupDescField');
			var $errorLabel = $('#DBMigratorNameDescriptionPopup .input-error-label');
			var header = strData.header ? strData.header : 'Enter details';
			var title = strData.title ? strData.title : '';
			var description = strData.description ? strData.description : '';
				
			$nameField.val(title);
			$descField.val(description);
			$('#DBMigratorNameDescriptionPopup .modal-title').val(header);
			
			$DBMigratorNameDescriptionPopup.off('shown.bs.modal').on('shown.bs.modal', function () {
				$nameField.focus();
			});
			
			$DBMigratorNameDescriptionPopup.modal('show');
			
			$nameDescPopupSubmitBtn.off('click').on('click', function(){
				var bValid = true;
				var bErrorMsg = "<ul>";
				var strTitle = $nameField.val();
				var strDesc = $descField.val();
				
				if(strTitle.trim() == ""){
					bValid = false;
					bErrorMsg += '<li>Please enter valid name</li>';
				} 
				if(strDesc.trim() == ""){
					bValid = false;
					bErrorMsg += '<li> Please enter valid description</li>';
				} 
				bErrorMsg += "</ul>";
				if(!bValid){
					$errorLabel.html(bErrorMsg);
					$errorLabel.show();
				}else {
					var sendJS = {
							Name: strTitle,
							Desc: strDesc
						};
					$nameField.val('');
					$descField.val('');
					$errorLabel.html('');
					$errorLabel.hide();
					$nameDescPopupSubmitBtn.off('click');
					$DBMigratorNameDescriptionPopup.modal('hide');
					if(fnCallback){
						fnCallback(sendJS);
					}
					
				}
			})
		},
		
		getInputPopup: function(title, fnCallback, value){
			this.setInfoPopupMode(false);
			this.updateInfoPopup(title);
			this.closeCallBack = fnCallback;
			if(value){
				$PopupInputField.val(value);
			} else {
				$PopupInputField.val('');
			}
			$DBMigratorPopup.off('shown.bs.modal').on('shown.bs.modal', function () {
			    $PopupInputField.focus();
			});
			$DBMigratorPopup.modal('show');
			$InputPopupSubmitBtn.off('click').on('click', function(){
				var strData = $PopupInputField.val();
				if(strData.trim() == ""){
					// SHOW SOMW INVALID ERROR
					$DBMigratorPopupErrorLabel.text("Please enter valid name").show();
				} else {
					$DBMigratorPopupErrorLabel.text("").hide();
					$InputPopupSubmitBtn.off('click');
					$DBMigratorPopup.modal('hide');
					if(fnCallback){
						fnCallback(strData);
					}
				}
			})
		},
		
		getTableRelationPopup: function(popupData){
			this.showCustomPopup('DBMigratorTableRelationPopup',popupData);
		},
		
		showCustomPopup: function(selector, data){
			if($('#'+selector).length == 0){
				console.error('JQuery popups not loaded !! :( ');
			}
			$('#'+selector).modal('show');
			var $migratorPopupScope = angular.element($("#"+selector+" .modal-content")).scope();
			$migratorPopupScope.setData(data);
		},
		
		showSelectionPopup: function(popupData){
			this.showCustomPopup('DBMigratorSelectorPopup',popupData);
		},
		
		/*showSelectIdentifiersPopup: function(popupData){
			this.showCustomPopup('DBMigratorIdentifierSelectorPopup',popupData);
		},*/
		
		showBigHtmlInfoPopup: function(popupData){
			this.showCustomPopup('DBMigratorBigInfoPopup',popupData);
		},
		
		showProgressPopup: function(title){
			if($DBMigratorSmallPopup.length == 0){
				/* TODO : Find a better way to calculate DOM elements on load of application. This is just a workaround as this pop up is called first  */
				calcDomElements();
			}
			var ttl = title ? title : 'Loading..';
			$('#DBMigratorSmallPopup .modal-title').empty().append(ttl);
			$DBMigratorSmallPopup.modal('show');
		},
		
		closeProgressPopup: function(){
			$DBMigratorSmallPopup.modal('hide');
		},
		
		getRandomText: function (){
		    var text = "";
		    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

		    for( var i=0; i < 5; i++ )
		        text += possible.charAt(Math.floor(Math.random() * possible.length));

		    return text;
		},
	};
});


/*DBMigratorApp.controller('popupInitializer', function ($scope, $interval, popupFactory, $timeout, dbUtilities){
	popupFactory.initPopupLogic();
});*/

DBMigratorApp.controller('selectOptionPopup', function ($scope, $interval, popupFactory, $timeout, dbUtilities, $element){
	$scope.setData = function(data){
		$scope.options = data.options;
		$scope.fnCallback = data.fnCallback;
		$scope.title = data.title ? data.title : 'Select Option';
		$scope.migratTypeSelectMode = data.migratTypeSelectMode ? data.migratTypeSelectMode : false;
	};
	
	$scope.backToHome = function(){
		$('#DBMigratorSelectorPopup').modal('hide');
		window.location.hash = '#/Home';
	}
	
	$scope.selectOption = function(option){
		$('#DBMigratorSelectorPopup').modal('hide');
		if($scope.fnCallback){
			$scope.fnCallback(option.value);
		}
	};
	$scope.tablesRelationDataScope = $scope;
	$scope.fetchedtableData = {columns:[], tables: []};
	
});

DBMigratorApp.controller('identifiersSelectPopup', function ($scope, $interval, popupFactory, $timeout, dbUtilities){
	$scope.setData = function(data){
		$scope.element = data.element;
		//$scope.fnCallback = data.fnCallback;
		$scope.title = data.title ? data.title : 'Select Option';
	};
});

/* NOTE : This popup is not in use yet !! */
DBMigratorApp.controller('bigInfoPopup', function ($scope, $interval, popupFactory, $timeout, dbUtilities, $sce){
	$scope.setData = function(data){
		$scope.htmlMessage = $sce.trustAsHtml(data.htmlContent);
		$scope.title = data.title ? data.title : 'Select Option';
	};
});



DBMigratorApp.factory('ExpUtilities', function (){
	return {
		getMongoAttrTypes: function(columnType){
			return nDBMappings[columnType];
		},
		
		getDefaultAttributeType: function(columnType){
			return nDBMappings[columnType][0];
		}
	};
});

DBMigratorApp.factory('dbUtilities', function (){
	var nDBMappings = {
			NUMBER: ['NUMBER','STRING','INTEGER','DOUBLE'],
			VARCHAR: ['STRING'],
			VARCHAR2: ['STRING'],
			CHAR: ['STRING'],
			DATE: ['DATE','STRING'],
			TIMESTAMP: ['DATE','STRING'],
			CLOB: ['STRING'],
			BLOB: ['B_ARRAY']
	};
	
	var columnMatchLiteralMapping = {
			NUMBER: 'NUMBER',
			VARCHAR: 'VARCHAR',
			VARCHAR2: 'VARCHAR',
			CHAR: 'CHAR',
			DATE: 'DATE',
			TIMESTAMP: 'DATE',
/*			CLOB: 'STRING',
			BLOB: 'STRING'*/
	}
	
	var validCopareColumnType = ["NUMBER","VARCHAR","VARCHAR2","CHAR","DATE","TIMESTAMP"]
	
	var currentJSON = null;
	
	var tableAlias = {};
	var tableAliasColumnMap = {};
	var aliasCounter = 0;
	return {
		getMongoAttrTypes: function(columnType){
			return nDBMappings[columnType];
		},
		
		isComparableColumnType: function(columnType){
			return (validCopareColumnType.indexOf(columnType) > -1);
		},
		
		getDefaultAttributeType: function(columnType){
			return nDBMappings[columnType][0];
		},
		
		getColumnMatchLiteralType: function(columnType){
			if(columnMatchLiteralMapping[columnType]){
				return columnMatchLiteralMapping[columnType];
			} else {
				console.warn('Matching literal type not found for column of Type : '+columnType);
				return false;
			}
		},
		
		copyArray: function(arr1, arr2){
			for (var i = 0; i < arr1.length; i++) {
				arr2[i] = arr1[i];
			}
		},
		
		appendArray: function(arr1, arr2){
			for (var i = 0; i < arr1.length; i++) {
				arr2.push(arr1[i]);
			}
		},
		
		registerTable: function(tableName){
			if(!tableAlias[tableName]){
				tableAlias[tableName] = "T"+aliasCounter;
				tableAliasColumnMap["T"+aliasCounter] = tableName;
				aliasCounter++;
			}
		},
		
		setTableAlias: function(strTableName, strAlias){
			if(tableAlias[strTableName] && strAlias != tableAlias[strTableName]){
				// TODO : check this functionality ... It can break in some cases
			} else {
				tableAlias[strTableName] = strAlias;
			}
			tableAliasColumnMap[strAlias] = strTableName;
		},
		
		setSameNameFromAlias: function(strTableName, strAlias){
			tableAliasColumnMap[strAlias] = strTableName;
		},
		
		clearAliasData: function(){
			tableAlias = {};
			tableAliasColumnMap = {};
		},
		
		getTableFromAlias: function(strTableAlias){
			return tableAliasColumnMap[strTableAlias];
		},
		
		getTableAlias: function(tableName, suffix){
			if(!tableAlias[tableName]){
				var Alias = "T"+aliasCounter;
				if(tableAliasColumnMap["T"+aliasCounter]){
					do{
						aliasCounter++;
					} while (tableAliasColumnMap["T"+aliasCounter])
				}
				tableAlias[tableName] = "T"+aliasCounter;
				tableAliasColumnMap["T"+aliasCounter] = tableName;
				aliasCounter++;
			} 
			if(suffix){
				if(tableAlias[tableName]){
					return tableAlias[tableName]+suffix;
				}
			}
			return tableAlias[tableName];
			
		},
		
		setCurrentJSON: function(jsonData){
			currentJSON = jsonData;
		},
		
		isCurrentJSON: function(){
			if(currentJSON){
				return true;
			} else {
				return false;
			}
		},
		
		fetchCurrentJSON: function(){
			var returnJSON = jQuery.extend({}, currentJSON);
			currentJSON = null;
			return returnJSON;
		},
		
		isCollectionType: function(str){
			if(str == "AUTO" || str == "COLLECTION" || str == "ARRAY"){
				return true;
			}
			return false;
		},
		
		sortAttribute: function(a,b) {
			  if((a.attributeType == "AUTO" || a.attributeType == "COLLECTION" || a.attributeType == "ARRAY") && (b.attributeType == "AUTO" || b.attributeType == "COLLECTION" || b.attributeType == "ARRAY")){
				  if (a.attributeName < b.attributeName)
				    return -1;
				  else if (a.attributeName > b.attributeName)
				    return 1;
				  else 
				    return 0;
			  }
			  else if(a.attributeType == "AUTO" || a.attributeType == "COLLECTION" || a.attributeType == "ARRAY")
				  return 1;
			  else if(b.attributeType == "AUTO" || b.attributeType == "COLLECTION" || b.attributeType == "ARRAY")
				  return -1;
			  else if (a.attributeName < b.attributeName)
				  return -1;
			  else if (a.attributeName > b.attributeName)
				  return 1;
			  else 
			    return 0;
		},
		
		populateJavaCollection: function(mainCollection, oCollection, isPrimary, insertParent, parentName, isSortingNeeded){
			var nodeName = oCollection.attributeName;
			mainCollection.attributeName = oCollection.attributeName;
			if(isPrimary){
				//mainCollection.isRootCollection = true;
			} else {
				mainCollection.attributeType = oCollection.attributeType ? oCollection.attributeType : "COLLECTION";
				if(parentName){
					nodeName = parentName + '.' + nodeName;
				}
			}
			mainCollection.attrs = [];
			mainCollection.currentAttribTables = oCollection.currentAttribTables;
			mainCollection.referencedColumns = oCollection.referencedColumns;
			for (var i = 0; i < oCollection.attributes.length; i++) {
				
				if(this.isCollectionType(oCollection.attributes[i].attributeType)){
					var subCollection = {};
					mainCollection.attrs.push(subCollection);
					this.populateJavaCollection(subCollection, oCollection.attributes[i], false, insertParent, nodeName, isSortingNeeded)
					// Handle new collection
				} else {
					if(insertParent){
						oCollection.attributes[i].parentCollection = nodeName;
					}
					mainCollection.attrs.push(oCollection.attributes[i]);
				}	
			}
			if(oCollection.identifiers && oCollection.identifiers.length > 0){
				for (var j = 0; j < oCollection.identifiers.length; j++) {
					var array_element = $.extend({},oCollection.identifiers[j]);
					array_element.isIdentifier = true;
					if(insertParent){
						array_element.parentCollection = nodeName;
					}
					mainCollection.attrs.push(array_element);
				}
			}
			if(mainCollection.attrs.length > 0 && isSortingNeeded){
				mainCollection.attrs.sort(this.sortAttribute);
			}
			mainCollection.sourceTables = oCollection.sourceTables;
			mainCollection.filters = oCollection.filters ? oCollection.filters : [];
		}
	};
});



DBMigratorApp.factory('devUtilities', function (){
	var isDebugMode = false;
	return {
		isDebugMode: function(){
			return isDebugMode;
		},
		
		resetDebugMode: function(){
			if(!isDebugMode){
				isDebugMode = true;
			} else {
				isDebugMode = false;
			}
		},
		
		 getDummyTablesData : function (){
			 return [{"name":"ORDER_ERROR_LOG","nodes":[{"columnName":"ERROR_DESC","columnType":"VARCHAR2","precision":4000,"isNullable":true,"isParentColumn":false},{"columnName":"ERROR_BY","columnType":"VARCHAR2","precision":255,"isNullable":true,"isParentColumn":false},{"columnName":"OBJECT_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"ERROR_NO","columnType":"VARCHAR2","precision":20,"isNullable":true,"isParentColumn":false},{"columnName":"WEB_ORDER_ID","columnType":"VARCHAR2","precision":100,"isNullable":true,"isParentColumn":false},{"columnName":"ERROR_TS","columnType":"DATE","precision":0,"isNullable":true,"isParentColumn":false}],"showtree":true},{"name":"ORDER_ORCH_APPLICATION_CONTEXT","nodes":[{"columnName":"DESCRIPTION","columnType":"VARCHAR2","precision":4000,"isNullable":true,"isParentColumn":false},{"columnName":"CATEGORY","columnType":"VARCHAR2","precision":100,"isNullable":true,"isParentColumn":false},{"columnName":"ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"VALUE","columnType":"VARCHAR2","precision":2000,"isNullable":true,"isParentColumn":false},{"columnName":"NAME","columnType":"VARCHAR2","precision":100,"isNullable":true,"isParentColumn":false}],"showtree":true}];
			 //return [{"name":"XXCAC_APPROVAL_HISTORY_ALL_QTC","nodes":[{"columnName":"APPROVER_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"SAF_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false}]},{"name":"XXCAC_APPR_TIMEOUT_HIST_QTC","nodes":[{"columnName":"PREV_ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"PREV_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"SAF_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"ORIG_ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"ORIG_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false}]},{"name":"XXCAC_DISPUTE_HEADERS_ALL_QTC","nodes":[{"columnName":"USER_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"ORG_ID","columnType":"NUMBER","precision":15,"isNullable":true,"isParentColumn":false},{"columnName":"COLLECTOR_ID","columnType":"NUMBER","precision":15,"isNullable":true,"isParentColumn":false}]},{"name":"XXCAC_FC_THEATRE_MASTER_QTC","nodes":[{"columnName":"CRM_GROUP_NAME","columnType":"VARCHAR2","precision":90,"isNullable":false,"isParentColumn":false},{"columnName":"CREATED_BY","columnType":"NUMBER","precision":15,"isNullable":false,"isParentColumn":false},{"columnName":"DB_INSTANCE_NAME","columnType":"VARCHAR2","precision":90,"isNullable":false,"isParentColumn":false},{"columnName":"LAST_UPDATED_BY","columnType":"NUMBER","precision":15,"isNullable":true,"isParentColumn":false},{"columnName":"ORG_ID","columnType":"NUMBER","precision":15,"isNullable":false,"isParentColumn":false},{"columnName":"THEATRE_NAME","columnType":"VARCHAR2","precision":90,"isNullable":false,"isParentColumn":false},{"columnName":"CREATION_DATE","columnType":"DATE","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"THEATRE_DESC","columnType":"VARCHAR2","precision":300,"isNullable":true,"isParentColumn":false},{"columnName":"LAST_UPDATE_DATE","columnType":"DATE","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"CURRENCY_CODE","columnType":"VARCHAR2","precision":45,"isNullable":false,"isParentColumn":false}]},{"name":"XXCAC_SAF_APPROVER_LIST_QTC","nodes":[{"columnName":"SAF_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false}]},{"name":"XXCAC_SAF_HEADERS_ALL_QTC","nodes":[{"columnName":"ORG_ID","columnType":"NUMBER","precision":15,"isNullable":true,"isParentColumn":false},{"columnName":"COLLECTOR_ID","columnType":"NUMBER","precision":15,"isNullable":true,"isParentColumn":false},{"columnName":"SAF_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false},{"columnName":"INITIATOR_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"INITIATOR_ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false}]},{"name":"XXCAC_SAF_RULE_DETAILS_ALL_QTC","nodes":[{"columnName":"ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false},{"columnName":"SAF_RULE_HEADER_ID","columnType":"NUMBER","precision":0,"isNullable":false,"isParentColumn":false}]},{"name":"XXCAC_SAF_RULE_HEADERS_ALL_QTC","nodes":[]},{"name":"XXCAS_FMW_QUEUE_TABLE","nodes":[]},{"name":"XXCCA_AB_ORDER_LINES_ABC_AUDIT","nodes":[]},{"name":"XXCCA_FLAT_BOM_EXPLOSION_TEMP","nodes":[]},{"name":"XXCCA_JUNK2_116","nodes":[]},{"name":"XXCCA_JUNK2_1166","nodes":[]},{"name":"XXCCA_JUNK2_161","nodes":[]},{"name":"XXCCA_JUNK2_611","nodes":[]},{"name":"XXCCA_MRG_MSG_LOG_BKP","nodes":[]},{"name":"XXCCA_MRG_SITE_USE_MAP_TEST","nodes":[]},{"name":"XXCCA_OE_ORDER_HEADERS_EXT","nodes":[]},{"name":"XXCCA_OE_ORDER_LINES_EXT","nodes":[]},{"name":"XXCCA_OMR12_BUS_PRE_MIGRN","nodes":[]},{"name":"XXCCA_OM_BUS_R12_PRE_MIGRN","nodes":[]},{"name":"XXCCA_OM_PRE_MIGRN_R12_AUDIT","nodes":[]},{"name":"XXCCA_OM_R12_PRE_MIGRN_BKP1","nodes":[]},{"name":"XXCCA_PRE_MIGRN_R12_AUDIT_BKP","nodes":[]},{"name":"XXCCA_SRC_CONFIG_LANES","nodes":[]},{"name":"XXCCA_UKH_ORD_HDRS_VAL_AUD","nodes":[]},{"name":"XXCCP_DEBUG_MESSAGES_TEMP","nodes":[]},{"name":"XXCCP_DEBUG_MESSAGES_TEMP1","nodes":[]},{"name":"XXCCP_DUMMY_TEST","nodes":[]},{"name":"XXCCP_ERP_LINE_STATUS_LOOKUP","nodes":[]},{"name":"XXCCP_ITDS_AUDIT_HISTORY","nodes":[]},{"name":"XXCCP_OC_QUERIES","nodes":[]},{"name":"XXCCP_OC_UI_INFO","nodes":[]},{"name":"XXCCP_OE_ORDER_HEADERS_ALL_TT","nodes":[]},{"name":"XXCCP_OE_ORDER_HEADERS_T","nodes":[]},{"name":"XXCCP_OO_SOURCE_HIST","nodes":[]},{"name":"XXCCP_ORDER_HEADERS_STG","nodes":[]},{"name":"XXCCP_ORDER_LINES_STG","nodes":[]},{"name":"XXCCP_SAAS_BILLING_TFS_DETAILS","nodes":[]},{"name":"XXCCP_SCMTEST","nodes":[]},{"name":"XXCCP_T1","nodes":[]},{"name":"XXCCP_T2","nodes":[]},{"name":"XXCCP_T3","nodes":[]},{"name":"XXCCP_T4","nodes":[]},{"name":"XXCCP_TASK_DETAILS","nodes":[]},{"name":"XXCFIADJERR","nodes":[]},{"name":"XXCFIF_FA_MASS_TRANSFER_TAB","nodes":[]},{"name":"XXCFIG_GGREP_LOG_21SEP15","nodes":[]},{"name":"XXCFIOFFATT_VW","nodes":[]},{"name":"XXCFIR_2TIER_LOG","nodes":[]},{"name":"XXCFIR_ACCT_BAL_SLA_TEMP","nodes":[]},{"name":"XXCFIR_AF_GL_DIST_BKP1","nodes":[]},{"name":"XXCFIR_B2B_INTERFACE_HEADER_B","nodes":[]},{"name":"XXCFIR_CAPITAL_TRX_DETAILS","nodes":[]},{"name":"XXCFIR_CAPITAL_TRX_DETAILS1","nodes":[]},{"name":"XXCFIR_CM_DM_OFFSET","nodes":[]},{"name":"XXCFIR_CM_ONACCOUNT","nodes":[]},{"name":"XXCFIR_CN_IN_TR_LNS_AL_1212015","nodes":[]},{"name":"XXCFIR_COLL_SETUP_LOG_BKUP","nodes":[]},{"name":"XXCFIR_COLL_SETUP_LOG_BKUP1","nodes":[]},{"name":"XXCFIR_CONS_INV_TRX_ALL_121205","nodes":[]},{"name":"XXCFIR_CUSTOMER_TRX_LINES_EXT","nodes":[]},{"name":"XXCFIR_CUST_TRX_HEADERS_STG_T","nodes":[]},{"name":"XXCFIR_DM_ACCOUNT_RULES_1","nodes":[]},{"name":"XXCFIR_DM_BVPROD_DFF_DATA","nodes":[]},{"name":"XXCFIR_DM_CG1PROD_DFF_DATA","nodes":[]},{"name":"XXCFIR_DM_CREDIT_PROFILE","nodes":[]},{"name":"XXCFIR_DM_LOCKBOXES","nodes":[]},{"name":"XXCFIR_DM_LOOKUPS_BKUP","nodes":[]},{"name":"XXCFIR_DM_MENU_BKUP","nodes":[]},{"name":"XXCFIR_DM_MENU_TEMP","nodes":[]},{"name":"XXCFIR_DM_NL_ITL_JPN_CNT","nodes":[]},{"name":"XXCFIR_DM_NL_ITL_JPN_CNT1","nodes":[]},{"name":"XXCFIR_DM_ORG_BV_TABLES","nodes":[]},{"name":"XXCFIR_DM_ORG_QTC_TABLES","nodes":[]},{"name":"XXCFIR_DM_QTCBV_CUSTOM_TABLES","nodes":[]},{"name":"XXCFIR_DM_QTCBV_CUSTOM_TABLES1","nodes":[]},{"name":"XXCFIR_DM_RECEIPT_CLASSES","nodes":[]},{"name":"XXCFIR_DM_RESPONSIBILITIES1","nodes":[]},{"name":"XXCFIR_DM_TEST","nodes":[]},{"name":"XXCFIR_DM_UKH_TABLES","nodes":[]},{"name":"XXCFIR_DM_UKH_USCAN_TABLES","nodes":[]},{"name":"XXCFIR_DST_ACCOUNT_RULES_1","nodes":[]},{"name":"XXCFIR_EDEL_HDR_TEMP","nodes":[]},{"name":"XXCFIR_EDEL_LINE_TEMP","nodes":[]},{"name":"XXCFIR_EDI_845_XML_DATA1","nodes":[]},{"name":"XXCFIR_EDI_INBOUND_INTERFACE1","nodes":[]},{"name":"XXCFIR_EDI_INBOUND_INTERFACE2","nodes":[]},{"name":"XXCFIR_EDI_INBOUND_INTERFACE3","nodes":[]},{"name":"XXCFIR_EDI_INBOUND_INTERFACE4","nodes":[]},{"name":"XXCFIR_ICF_CONSOLIDATION_RULES","nodes":[]},{"name":"XXCFIR_ICMS_CM_AMORT_FIX","nodes":[]},{"name":"XXCFIR_IPM_REPROCESS_BKP","nodes":[]},{"name":"XXCFIR_OA_RDR_TEST","nodes":[]},{"name":"XXCFIR_PRINT_INVOICE_TEMP1","nodes":[]},{"name":"XXCFIR_PROD_FAMILY_MV","nodes":[]},{"name":"XXCFIR_RA_CUST_TRX_EXTN","nodes":[]},{"name":"XXCFIR_RCTLGDA_TMP1","nodes":[]},{"name":"XXCFIR_REVENUE_DIST_ALL_BKP","nodes":[]},{"name":"XXCFIR_REVENUE_EXTRACT_ALL_BKP","nodes":[]},{"name":"XXCFIR_REVENUE_RECOG_RULES","nodes":[]},{"name":"XXCFIR_REVENUE_RULESBKP_JUN12","nodes":[]},{"name":"XXCFIR_REV_EXTRACT_STG_BKP","nodes":[]},{"name":"XXCFIR_RRR_OBJECTS","nodes":[]},{"name":"XXCFIR_SBP_TRX_HEADERS_STG_TMP","nodes":[]},{"name":"XXCFIR_SBP_TRX_LINES_STG_TMP","nodes":[]},{"name":"XXCFIR_SKU_MV","nodes":[]},{"name":"XXCFIR_SRC_ACCOUNT_RULES_1","nodes":[]},{"name":"XXCFIR_TEMP1","nodes":[]},{"name":"XXCFIR_TEMP116","nodes":[]},{"name":"XXCFIR_TEST","nodes":[]},{"name":"XXCFIR_TEST123","nodes":[]},{"name":"XXCFIR_TEST_BI","nodes":[]},{"name":"XXCFIR_TEST_XML","nodes":[]},{"name":"XXCFIR_TS4_CREDIT_PROFILES","nodes":[]},{"name":"XXCFIR_TS4_MASS_ALLOCATION","nodes":[]},{"name":"XXCFIR_TS4_SYSTEM_OPTIONS","nodes":[]},{"name":"XXCFIR_TS4_VALUE_SETS","nodes":[]},{"name":"XXCFIR_XAAS_INV_DETAILS_240714","nodes":[]},{"name":"XXCFIR_XAAS_INV_HEADERS_100914","nodes":[]},{"name":"XXCFIR_XAAS_INV_HEADERS_PRANAV","nodes":[]},{"name":"XXCFIR_XAAS_INV_HEADERS_TEST","nodes":[]},{"name":"XXCFIR_XAAS_INV_TAX_LINES_2407","nodes":[]},{"name":"XXCFIT_CAN_DETAIL_TB12","nodes":[]},{"name":"XXCFIT_CN_RPT_TMP_TBL","nodes":[]},{"name":"XXCFIT_CUST_VAT_NUMBERS_BKP","nodes":[]},{"name":"XXCFIT_HOLDS_ACTIONS_ALL_BKP","nodes":[]},{"name":"XXCFIT_INVADJ_TEMP","nodes":[]},{"name":"XXCFIT_MAX_TRX_ID","nodes":[]},{"name":"XXCFIT_TB_INVOICE_LINES_MV","nodes":[]},{"name":"XXCFIT_TB_INV_LINE_TAXES_MV","nodes":[]},{"name":"XXCFIT_TB_USERS_DUMMY","nodes":[]},{"name":"XXCFIT_TEST","nodes":[]},{"name":"XXCFIT_US_DETAIL_TB12","nodes":[]},{"name":"XXCFIT_US_SALES_TAX_RPT_TB","nodes":[]},{"name":"XXCFIT_VAT_VIES_RPT_TEMP","nodes":[]},{"name":"XXCFIT_VAT_VIES_XML_TEMP","nodes":[]},{"name":"XXCFIXXCFIT_AR_TAX_CODES_BKUP","nodes":[]},{"name":"XXCFIX_TOAD_USERS_DET","nodes":[]},{"name":"XXCFI_662784_TEST","nodes":[]},{"name":"XXCFI_AE_HDR_TEMP","nodes":[]},{"name":"XXCFI_AE_HEADERS_TEMP","nodes":[]},{"name":"XXCFI_AE_TEMP","nodes":[]},{"name":"XXCFI_AR_REPORT_QUERY","nodes":[]},{"name":"XXCFI_BV","nodes":[]},{"name":"XXCFI_CCU_ELA_HEADER_MAP_BK","nodes":[]},{"name":"XXCFI_CCU_ELA_HR_COUNTRIES_BK","nodes":[]},{"name":"XXCFI_CCU_SHIPPING_RULES","nodes":[]},{"name":"XXCFI_CCU_SHIPPING_RULES_BKP","nodes":[]},{"name":"XXCFI_CC_PAYMENT_HEADERS_ALL","nodes":[]},{"name":"XXCFI_CC_PAYMENT_LINES_ALL","nodes":[]},{"name":"XXCFI_CDBU_ACCT_ITEM_MV","nodes":[]},{"name":"XXCFI_CLL_F032_SUBINV_LAYERS","nodes":[]},{"name":"XXCFI_CONS_BILLING_DETAILS","nodes":[]},{"name":"XXCFI_CONS_BILLING_HEADER_TEST","nodes":[]},{"name":"XXCFI_CONS_BILLING_HEADER_TST","nodes":[]},{"name":"XXCFI_CONS_CUSTOMER_BIDS","nodes":[]},{"name":"XXCFI_CST_AE_HEADERS_MAY27","nodes":[]},{"name":"XXCFI_CST_AE_LINES_MAY27","nodes":[]},{"name":"XXCFI_CST_DUMMY","nodes":[]},{"name":"XXCFI_CST_HEAD_POST_CHGESMAY27","nodes":[]},{"name":"XXCFI_CST_HEAD_POST_CHGESMAY29","nodes":[]},{"name":"XXCFI_CST_HEAD_PRE_CHGESMAY27","nodes":[]},{"name":"XXCFI_CST_HEAD_PRE_CHGESMAY29","nodes":[]},{"name":"XXCFI_CST_LINE_POST_CHGESMAY27","nodes":[]},{"name":"XXCFI_CST_LINE_POST_CHGESMAY29","nodes":[]},{"name":"XXCFI_CST_LINE_PRE_CHGESMAY27","nodes":[]},{"name":"XXCFI_CST_LINE_PRE_CHGESMAY29","nodes":[]},{"name":"XXCFI_DFF_ANALYSIS_Q2FY16","nodes":[]},{"name":"XXCFI_DUMMY","nodes":[]},{"name":"XXCFI_EC_CL_HISTORY_C190914","nodes":[]},{"name":"XXCFI_EC_EXP_DTL_C190914","nodes":[]},{"name":"XXCFI_EC_EXP_SUM_C190914","nodes":[]},{"name":"XXCFI_EC_MASTER_C190914","nodes":[]},{"name":"XXCFI_EC_RELATIONSHIPS_C190914","nodes":[]},{"name":"XXCFI_EC_THEATER_CL_C190914","nodes":[]},{"name":"XXCFI_ELIG_INTERFCE_LOG","nodes":[]},{"name":"XXCFI_GL_MMT_IFACE_CONTROL_BKP","nodes":[]},{"name":"XXCFI_GL_PERIODS_INSERT","nodes":[]},{"name":"XXCFI_GL_RECON_TEMP","nodes":[]},{"name":"XXCFI_INSERT_EMP_RECORDS","nodes":[]},{"name":"XXCFI_LEDGER_XLA_MAPPING","nodes":[]},{"name":"XXCFI_LOG_DEBUG_MESSAGES_T","nodes":[]},{"name":"XXCFI_LT","nodes":[]},{"name":"XXCFI_MESSAGES","nodes":[]},{"name":"XXCFI_MTL_SYSTEM_ITEMS_B","nodes":[]},{"name":"XXCFI_OA_ATTRIBUTED_AMTS_MV","nodes":[]},{"name":"XXCFI_PACITEM_BCKUP_MAY27","nodes":[]},{"name":"XXCFI_PACTRNS_BCKUP_MAY27","nodes":[]},{"name":"XXCFI_PAC_ITEM_CHGESMAY27","nodes":[]},{"name":"XXCFI_PAC_ITEM_CHGESMAY29","nodes":[]},{"name":"XXCFI_PAC_ITEM_DETAILS","nodes":[]},{"name":"XXCFI_PAC_ITEM_DETAILS1","nodes":[]},{"name":"XXCFI_PAC_ITEM_DETAILS_TEST","nodes":[]},{"name":"XXCFI_PAC_ITEM_PRE_CHGESMAY27","nodes":[]},{"name":"XXCFI_PAC_ITEM_PRE_CHGESMAY29","nodes":[]},{"name":"XXCFI_PAC_TRANS_DETAILS","nodes":[]},{"name":"XXCFI_PAC_TRANS_DETAILS1","nodes":[]},{"name":"XXCFI_PAC_TRANS_DETAILS_TEST","nodes":[]},{"name":"XXCFI_PO_EMPLOYEE","nodes":[]},{"name":"XXCFI_PRE_REMIT_HEADERS1","nodes":[]},{"name":"XXCFI_RA_CUSTOMER_TRX_ALL","nodes":[]},{"name":"XXCFI_RPT_GENERIC_TEMP1","nodes":[]},{"name":"XXCFI_RPT_HIERARCHY_TEMP","nodes":[]},{"name":"XXCFI_RTM_HEADERS","nodes":[]},{"name":"XXCFI_SPECIAL_ORDER_MV","nodes":[]},{"name":"XXCFI_SUBINVLAYER_MAY27","nodes":[]},{"name":"XXCFI_SUBINV_POST_CHGESMAY27","nodes":[]},{"name":"XXCFI_SUBINV_POST_CHGESMAY29","nodes":[]},{"name":"XXCFI_SUBINV_PRE_CHGESMAY27","nodes":[]},{"name":"XXCFI_SUBINV_PRE_CHGESMAY29","nodes":[]},{"name":"XXCFI_TEMP1","nodes":[]},{"name":"XXCFI_TEMP_810","nodes":[]},{"name":"XXCFI_TEMP_FAR","nodes":[]},{"name":"XXCFI_TEST","nodes":[]},{"name":"XXCFI_TEST234","nodes":[]},{"name":"XXCFI_TEST_SUPP_DEV","nodes":[]},{"name":"XXCFI_TRANS_POST_CHGESMAY27","nodes":[]},{"name":"XXCFI_TRANS_POST_CHGESMAY29","nodes":[]},{"name":"XXCFI_TRANS_PRE_CHGESMAY27","nodes":[]},{"name":"XXCFI_TRANS_PRE_CHGESMAY29","nodes":[]},{"name":"XXCFI_VT_ACC_PC_HIER_MV","nodes":[]},{"name":"XXCFI_VT_AR_ADJ_BATCH_SOURCE","nodes":[]},{"name":"XXCFI_VT_AR_ADJ_REV_ACCOUNTS","nodes":[]},{"name":"XXCFI_VT_AR_TEST_TAB","nodes":[]},{"name":"XXCFI_VT_BV_OM_TMP","nodes":[]},{"name":"XXCFI_VT_CCU_SHIPPING_RULES","nodes":[]},{"name":"XXCFI_VT_CCU_SWF_ATTR_WB","nodes":[]},{"name":"XXCFI_VT_CF_GL_LEDGERS_MV","nodes":[]},{"name":"XXCFI_VT_COMPID_SABRIX_MV","nodes":[]},{"name":"XXCFI_VT_CP_CUST_DATA_MV","nodes":[]},{"name":"XXCFI_VT_DEPT_PC_HIER_MV","nodes":[]},{"name":"XXCFI_VT_DUMMY_TEMP","nodes":[]},{"name":"XXCFI_VT_EVENT_INT_BAKUP","nodes":[]},{"name":"XXCFI_VT_GCC_REV_ACC_MV","nodes":[]},{"name":"XXCFI_VT_GL_CODE_COMB_MV","nodes":[]},{"name":"XXCFI_VT_GL_FSG_DATA_TEMP","nodes":[]},{"name":"XXCFI_VT_IC_CAT_0226","nodes":[]},{"name":"XXCFI_VT_IC_TAX_CATEGORIES_BKP","nodes":[]},{"name":"XXCFI_VT_IC_TAX_CATEGORIES_BKT","nodes":[]},{"name":"XXCFI_VT_IC_TAX_CATEGORIES_TMP","nodes":[]},{"name":"XXCFI_VT_RAE_ID_MAPPING","nodes":[]},{"name":"XXCFI_VT_RUSSIA_IPL_ERRORS","nodes":[]},{"name":"XXCFI_VT_SC_INTERFACE1","nodes":[]},{"name":"XXCFI_VT_SI_TAX_CAT_MV","nodes":[]},{"name":"XXCFI_VT_SRC_ASSGN_TABLE","nodes":[]},{"name":"XXCFI_VT_TEMP","nodes":[]},{"name":"XXCFI_VT_TEST_DATA_JAN_SEP15","nodes":[]},{"name":"XXCFI_VT_XLA_DTLS_DUMMY","nodes":[]},{"name":"XXCFI_VT_XLA_TEST_TAB","nodes":[]},{"name":"XXCFI_VT_YTD_GL_BAL1205","nodes":[]},{"name":"XXCFI_VT_YTD_GL_BAL_BKP0617","nodes":[]},{"name":"XXCFI_XLA_AE_LINES_ITL_MV","nodes":[]},{"name":"XXCFI_XYZ","nodes":[]},{"name":"XXCMF_HEADER_ID_QT","nodes":[]},{"name":"XXCMR_ERROR_LOG","nodes":[]},{"name":"XXCPD_B2B_BOOTSTRAP_FILES_COPY","nodes":[]},{"name":"XXCPD_B2B_BOOTSTRAP_FILES_TEST","nodes":[]},{"name":"XXCPD_CPY_B2B_BOOTSTRAP","nodes":[]},{"name":"XXCPD_DEBUG","nodes":[]},{"name":"XXCPD_DEBUG_TEXT","nodes":[]},{"name":"XXCPD_FACTOR_MASTER1","nodes":[]},{"name":"XXCPD_FACTOR_MODIFIED","nodes":[]},{"name":"XXCPD_INDIA_MOD_SETUP_TEMP","nodes":[]},{"name":"XXCPD_ITEM_DETAILS_SAN","nodes":[]},{"name":"XXCPD_ITEM_SER_TEST","nodes":[]},{"name":"XXCPD_PID_OWNER","nodes":[]},{"name":"XXCP_ACTUAL_COSTSDEC09","nodes":[]},{"name":"XXCP_CUST_DATA_BKP","nodes":[]},{"name":"XXCP_CUST_DATA_MV","nodes":[]},{"name":"XXCP_SUMMARIZED_DIST_IFACE_BKP","nodes":[]},{"name":"XXCP_VT_INTERFACE_DUMM","nodes":[]}]
		 },
		
		tempColDataJSON : function(i){
			var cols = [
			            [{"columnName":"PREV_ROLE_ID","columnType":"NUMBER","precision":0,"isNullable":true},{"columnName":"PREV_GROUP_ID"
				,"columnType":"NUMBER","precision":0,"isNullable":true},{"columnName":"SAF_ID","columnType":"NUMBER"
					,"precision":0,"isNullable":false},{"columnName":"ORIG_ROLE_ID","columnType":"NUMBER","precision":0,"isNullable"
					:true},{"columnName":"ORIG_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true}],
			
			            [{"columnName":"USER_GROUP_ID","columnType":"NUMBER","precision":0,"isNullable":true},{"columnName":"ORG_ID"
				,"columnType":"NUMBER","precision":15,"isNullable":true},{"columnName":"COLLECTOR_ID","columnType":"NUMBER"
				,"precision":15,"isNullable":true}],
			
			            [{"columnName":"CRM_GROUP_NAME","columnType":"VARCHAR2","precision":90,"isNullable":false},{"columnName"
				:"CREATED_BY","columnType":"NUMBER","precision":15,"isNullable":false},{"columnName":"DB_INSTANCE_NAME"
				,"columnType":"VARCHAR2","precision":90,"isNullable":false},{"columnName":"LAST_UPDATED_BY","columnType"
				:"NUMBER","precision":15,"isNullable":true},{"columnName":"ORG_ID","columnType":"NUMBER","precision"
				:15,"isNullable":false},{"columnName":"THEATRE_NAME","columnType":"VARCHAR2","precision":90,"isNullable"
				:false},{"columnName":"CREATION_DATE","columnType":"DATE","precision":0,"isNullable":false},{"columnName"
				:"THEATRE_DESC","columnType":"VARCHAR2","precision":300,"isNullable":true},{"columnName":"LAST_UPDATE_DATE"
				,"columnType":"DATE","precision":0,"isNullable":true},{"columnName":"CURRENCY_CODE","columnType":"VARCHAR2"
				,"precision":45,"isNullable":false}],
			
			            [{"columnName":"SAF_ID","columnType":"NUMBER","precision":0,"isNullable":false},{"columnName":"ROLE_ID"
				,"columnType":"NUMBER","precision":0,"isNullable":true}],
			];
			
			if(i < cols.length){
				return cols[i];
			} else {
				return [];
			}
		},
		tempTableJSON: function(){
			return ["XXCAC_APPROVAL_HISTORY_ALL_QTC","XXCAC_APPR_TIMEOUT_HIST_QTC","XXCAC_DISPUTE_HEADERS_ALL_QTC","XXCAC_FC_THEATRE_MASTER_QTC"
		        ,"XXCAC_SAF_APPROVER_LIST_QTC","XXCAC_SAF_HEADERS_ALL_QTC","XXCAC_SAF_RULE_DETAILS_ALL_QTC","XXCAC_SAF_RULE_HEADERS_ALL_QTC"
		        ,"XXCCA_AB_ORDER_LINES_ABC_AUDIT","XXCCA_FLAT_BOM_EXPLOSION_TEMP","XXCCA_JUNK2_116","XXCCA_JUNK2_1166"
		        ,"XXCCA_JUNK2_161","XXCCA_JUNK2_611","XXCCA_MRG_SITE_USE_MAP_TEST","XXCCA_OE_ORDER_HEADERS_EXT","XXCCA_OE_ORDER_LINES_EXT"
		        ,"XXCCA_OMR12_BUS_PRE_MIGRN","XXCCA_OM_BUS_R12_PRE_MIGRN","XXCCA_OM_PRE_MIGRN_R12_AUDIT","XXCCA_OM_R12_PRE_MIGRN_BKP1"
		        ,"XXCCA_PRE_MIGRN_R12_AUDIT_BKP","XXCCA_SRC_CONFIG_LANES","XXCCA_UKH_ORD_HDRS_VAL_AUD","XXCCP_DUMMY_TEST"
		        ,"XXCCP_ERP_LINE_STATUS_LOOKUP","XXCCP_ITDS_AUDIT_HISTORY","XXCCP_OC_QUERIES","XXCCP_OC_UI_INFO","XXCCP_OE_ORDER_HEADERS_ALL_TT"
		        ,"XXCCP_OE_ORDER_HEADERS_T","XXCCP_OO_SOURCE_HIST","XXCCP_ORDER_HEADERS_STG","XXCCP_ORDER_LINES_STG"
		        ,"XXCCP_SCMTEST","XXCCP_T1","XXCCP_T2","XXCCP_T3","XXCCP_T4","XXCCP_TASK_DETAILS","XXCFIADJERR","XXCFIF_FA_MASS_TRANSFER_TAB"
		        ,"XXCFIG_GGREP_LOG_21SEP15","XXCFIOFFATT_VW","XXCFIR_2TIER_LOG","XXCFIR_AF_GL_DIST_BKP1","XXCFIR_B2B_INTERFACE_HEADER_B"
		        ,"XXCFIR_CAPITAL_TRX_DETAILS","XXCFIR_CAPITAL_TRX_DETAILS1","XXCFIR_CM_DM_OFFSET","XXCFIR_CM_ONACCOUNT"
		        ,"XXCFIR_CN_IN_TR_LNS_AL_1212015","XXCFIR_COLL_SETUP_LOG_BKUP","XXCFIR_COLL_SETUP_LOG_BKUP1","XXCFIR_CONS_INV_TRX_ALL_121205"
		        ,"XXCFIR_CUSTOMER_TRX_LINES_EXT","XXCFIR_CUST_TRX_HEADERS_STG_T","XXCFIR_DM_ACCOUNT_RULES_1","XXCFIR_DM_BVPROD_DFF_DATA"
	        ];
		},
		
		getReverseCollection: function(){
			return {"attributeName":"DumyModeCollection","attrs":[{"attributeName":"APPROVER_ID","attributeType":"NUMBER","columnData":{"columnName":"APPROVER_ID","columnAlias":"T0_APPROVER_ID","columnType":"NUMBER","tableAlias":"T0"},"parentCollection":"DumyModeCollection"},{"attributeName":"ROLE_ID","attributeType":"NUMBER","columnData":{"columnName":"ROLE_ID","columnAlias":"T0_ROLE_ID","columnType":"NUMBER","tableAlias":"T0"},"parentCollection":"DumyModeCollection"},{"attributeName":"SAF_ID","attributeType":"NUMBER","columnData":{"columnName":"SAF_ID","columnAlias":"T0_SAF_ID","columnType":"NUMBER","tableAlias":"T0"},"parentCollection":"DumyModeCollection"},{"attributeName":"XXCAC_APPR_TIMEOUT_HIST_QTC","attributeType":"AUTO","attrs":[{"attributeName":"ORIG_GROUP_ID","attributeType":"NUMBER","columnData":{"columnName":"ORIG_GROUP_ID","columnAlias":"T1_ORIG_GROUP_ID","columnType":"NUMBER","tableAlias":"T1"}},{"attributeName":"ORIG_ROLE_ID","attributeType":"NUMBER","columnData":{"columnName":"ORIG_ROLE_ID","columnAlias":"T1_ORIG_ROLE_ID","columnType":"NUMBER","tableAlias":"T1"}},{"attributeName":"PREV_GROUP_ID","attributeType":"NUMBER","columnData":{"columnName":"PREV_GROUP_ID","columnAlias":"T1_PREV_GROUP_ID","columnType":"NUMBER","tableAlias":"T1"}},{"attributeName":"PREV_ROLE_ID","attributeType":"NUMBER","columnData":{"columnName":"PREV_ROLE_ID","columnAlias":"T1_PREV_ROLE_ID","columnType":"NUMBER","tableAlias":"T1"}},{"attributeName":"SAF_ID","attributeType":"NUMBER","columnData":{"columnName":"SAF_ID","columnAlias":"T1_SAF_ID","columnType":"NUMBER","tableAlias":"T1"}},{"attributeName":"XXCAC_DISPUTE_HEADERS_ALL_QTC","attributeType":"AUTO","attrs":[{"attributeName":"COLLECTOR_ID","attributeType":"NUMBER","columnData":{"columnName":"COLLECTOR_ID","columnAlias":"T2_COLLECTOR_ID","columnType":"NUMBER","tableAlias":"T2"}},{"attributeName":"ORG_ID","attributeType":"NUMBER","columnData":{"columnName":"ORG_ID","columnAlias":"T2_ORG_ID","columnType":"NUMBER","tableAlias":"T2"}},{"attributeName":"USER_GROUP_ID","attributeType":"NUMBER","columnData":{"columnName":"USER_GROUP_ID","columnAlias":"T2_USER_GROUP_ID","columnType":"NUMBER","tableAlias":"T2"}}],"sourceTables":[{"tableName":"XXCAC_DISPUTE_HEADERS_ALL_QTC","tableAlias":"T2"}],"filters":[{"sqlOperation":"EQ","leftHandExpression":{"expressionType":"column","columnData":{"columnName":"USER_GROUP_ID","columnAlias":"T2_USER_GROUP_ID","columnType":"NUMBER","tableAlias":"T2"}},"rightHandExpression":{"expressionType":"column","columnData":{"columnName":"SAF_ID","columnAlias":"T1_SAF_ID","columnType":"NUMBER","tableAlias":"T1","isParentColumn":true}}}]}],"sourceTables":[{"tableName":"XXCAC_APPR_TIMEOUT_HIST_QTC","tableAlias":"T1"}],"filters":[{"sqlOperation":"EQ","leftHandExpression":{"expressionType":"column","columnData":{"columnName":"PREV_ROLE_ID","columnAlias":"T1_PREV_ROLE_ID","columnType":"NUMBER","tableAlias":"T1"}},"rightHandExpression":{"expressionType":"column","columnData":{"columnName":"SAF_ID","columnAlias":"T0_SAF_ID","columnType":"NUMBER","tableAlias":"T0","isParentColumn":true}}}]}],"sourceTables":[{"tableName":"XXCAC_APPROVAL_HISTORY_ALL_QTC","tableAlias":"T0"}],"filters":[]};
		},
		
		getReverseTables: function(){
			return {"name":"ORDER_HEADER_C3","nodes":[{"columnName":"PROGRAM_TYPE","columnType":"VARCHAR2","precision":100,"isNullable":true,"isParentColumn":false,"isSeqGenerated":false},{"columnName":"ORDER_TYPE","columnType":"VARCHAR2","precision":100,"isNullable":true,"isParentColumn":false,"isSeqGenerated":false},{"columnName":"HEADER_ID","columnType":"NUMBER","precision":0,"isNullable":true,"isParentColumn":false,"isSeqGenerated":false}],"showtree":true};
		}
	};
});

DBMigratorApp.factory('Communicator', function (){
	return {
	};
});


DBMigratorApp.factory('DummyDataService', function (){
	return {
		getAdminPageDummyData: function(){
			return [
			    	{
			    		"node": "local",
			    		"host": "NPARDESH-9PYE8",
			    		"_id": {
			    			"$oid": "5757dfa996050ce83b81fe61"
			    		},
			    		"events": [{
			    			"createdOn": {
			    				"$date": 1465205356052
			    			},
			    			"mapId": {
			    				"$oid": "57553dbda378502b30894549"
			    			},
			    			"parallelReadInfo": {
			    				"processParallel": false,
			    				"numOfBuckets": 0
			    			},
			    			"createdBy": "npardesh",
			    			"_id": {
			    				"$oid": "5755426ca3785030c8a974f4"
			    			},
			    			"status": "CANCELLED",
			    			"parentEventId": {
			    				"$oid": "5755426ca3785030c8a974f4"
			    			},
			    			"batchSize": 100,
			    			"eventName": "error",
			    			"eventType": "OrclToMongo",
			    			"collectionName": "errors",
			    			"comments": "error",
			    			"mapName": "errors"
			    		},
			    		{
			    			"createdOn": {
			    				"$date": 1462338344605
			    			},
			    			"mapId": {
			    				"$oid": "5729829dd5b1492c54e32d05"
			    			},
			    			"parallelReadInfo": {
			    				"columnData": {
			    					"columnType": "VARCHAR2",
			    					"tableAlias": "T0",
			    					"columnName": "ERROR_MSG",
			    					"columnAlias": "T0_ERROR_MSG"
			    				},
			    				"processParallel": false,
			    				"numOfBuckets": 1
			    			},
			    			"createdBy": "npardesh",
			    			"_id": {
			    				"$oid": "57298328d5b1492c54e32d07"
			    			},
			    			"status": "COMPLETE",
			    			"parentEventId": {
			    				"$oid": "57298328d5b1492c54e32d07"
			    			},
			    			"batchSize": 100,
			    			"eventName": "logev",
			    			"eventType": "OrclToMongo",
			    			"collectionName": "log",
			    			"comments": "logev",
			    			"mapName": null
			    		}],
			    		"usedHeapSize": {
			    			"$numberLong": "279445555"
			    		},
			    		"totalHeapSize": {
			    			"$numberLong": "1840250990"
			    		},
			    		"lifeCycle": "dev",
			    		"systemEvents": [],
			    		"concurrencyLevel": 6,
			    		"eventTypes": []
			    	},
			    	{
			    		"node": "local",
			    		"host": "NPARDESH-9PYE8",
			    		"_id": {
			    			"$oid": "57581ecd9e9dfe1a64c21298"
			    		},
			    		"usedHeapSize": {
			    			"$numberLong": "279445504"
			    		},
			    		"totalHeapSize": {
			    			"$numberLong": "1840250880"
			    		},
			    		"lifeCycle": "dev",
			    		"systemEvents": [],
			    		"concurrencyLevel": 5,
			    		"eventTypes": []
			    	}];
		}
	};
});