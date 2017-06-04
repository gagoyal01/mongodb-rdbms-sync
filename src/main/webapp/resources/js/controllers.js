DBMigratorApp.controller('mainPageView', function($scope, popupFactory, $timeout, $rootScope, devUtilities){
	var filepaths = filesPath2;
	$scope.plugInTemplatesURL = filepaths+'/resources/templates/plugInTemplates.html';
	$scope.popupFactoryTemplatesURL = filepaths+'/resources/templates/popupFactoryTemplates.html';
	$scope.tableRelationTemplateURL = filepaths+'/resources/templates/tableRelationsEditorTemplate.html';
	
	$scope.isDebugMode = function(){
		return devUtilities.isDebugMode();
	},
	
	$scope.resetDebugMode = function(){
		devUtilities.resetDebugMode();
	}
	
	$scope.loadServices = function(){
		popupFactory.initPopupLogic();
	}
	
	$scope.init = function () {
		// If you want to initialize something !!
		
		var userdata = jsUserData;
		if(jsUserData){
			$rootScope.UserData = jsUserData;
			$rootScope.UserData.userRole = "Admin";
		}
	}
	
	$scope.init();
});

DBMigratorApp.controller('cleanUpControllerMixin', function($scope, popupFactory, $timeout){
	$scope.eventListeners = [];
	
	// Remove all event listeners on Destroy
	$scope.$on("$destroy", function() {
		for (var i = 0; i < $scope.eventListeners.length; i++) {
			$scope.eventListeners[i]();
		}
    });
});



DBMigratorApp.controller('autoCompleteColumnCtrlSimple', function($scope, popupFactory, $timeout, $controller, $q, $http){
	angular.extend(this, $controller('cleanUpControllerMixin', {$scope: $scope}));
	var self = this;

    self.simulateQuery = false;
    self.isDisabled    = false;

    // list of `state` value/display objects
    self.states        = loadAll();
    self.querySearch   = querySearch;
    self.selectedItemChange = selectedItemChange;
    self.searchTextChange   = searchTextChange;

    //self.newState = newState;
    
    $scope.eventListeners.push($scope.$on('dataloaded', function (e, columnValue) {
    	//console.log('DATALOADED : '+columnValue);
    	self.states  = loadAll();
    	if(columnValue){
    		self.searchText = columnValue;
    	} else {
    		self.searchText = '';
    	}
    	/* Sometimes when the list doesnt refresh */
    	$timeout(function () { $scope.$apply(); }, 0, false);
		//element.select2();
	}));

    // ******************************
    // Internal methods
    // ******************************

    /**
     * Search for elements... use $timeout to simulate
     * remote dataservice call.
     */
    function querySearch (query) {
    	var results = query ? self.states.filter( createFilterFor(query) ) : self.states;
    	return results;
    }
    
    /* When user is typing > Only update when field is empty */
    function searchTextChange(text) {
      if(text && text.trim() === ""){
    	  $scope.$emit('relation-column-selected', text, '');
      }
    }

    /* When user selects an item from list > update */
    function selectedItemChange(item) {
    	//console.log('AutoComplete >>> Selected item Changed');
    	//console.log(item);
    	if(item && item.label){
    		$scope.$emit('autocomplete-selected', item.label, item);
    	}
    }

    /**
     * Build `states` list of key/value pairs
     */
    function loadAll() {
    	if($scope.autoCompleteValues && $scope.autoCompleteValues.length > 0){
    		for (var i = 0; i < $scope.autoCompleteValues.length; i++) {
    			$scope.autoCompleteValues[i].value = $scope.autoCompleteValues[i].label.toLowerCase();
			}
    		//console.log($scope.autoCompleteValues);
    		return $scope.autoCompleteValues;
    	} else {
    		return [];
    	}
    }

    /**
     * Create filter function for a query string
     */
    function createFilterFor(query) {
      var lowercaseQuery = angular.lowercase(query);

      return function filterFn(state) {
        return (state.value.indexOf(lowercaseQuery) === 0);
      };

    }
});


DBMigratorApp.controller('autoCompleteColumnCtrl', function($scope, popupFactory, $timeout, $controller){
	angular.extend(this, $controller('cleanUpControllerMixin', {$scope: $scope}));
	var self = this;

    self.simulateQuery = false;
    self.isDisabled    = false;

    // list of `state` value/display objects
    self.states        = loadAll();
    self.querySearch   = querySearch;
    self.selectedItemChange = selectedItemChange;
    self.searchTextChange   = searchTextChange;
    //$scope.eventListeners = [];

    //self.newState = newState;
    
    $scope.eventListeners.push($scope.$on('dataloaded', function (e, columnValue) {
    	//console.log('DATALOADED : '+columnValue);
    	self.states  = loadAll();
    	if(columnValue){
    		self.searchText = columnValue;
    	} else {
    		self.searchText = '';
    	}
    	/* Sometimes when the list doesnt refresh */
    	$timeout(function () { $scope.$apply(); }, 0, false);
		//element.select2();
	}));

    /*function newState(state) {
      alert("Sorry! You'll need to create a Constituion for " + state + " first!");
    }*/

    // ******************************
    // Internal methods
    // ******************************

    /**
     * Search for states... use $timeout to simulate
     * remote dataservice call.
     */
    function querySearch (query) {
      var results = query ? self.states.filter( createFilterFor(query) ) : self.states,
          deferred;
      if (self.simulateQuery) {
        deferred = $q.defer();
        $timeout(function () { deferred.resolve( results ); }, Math.random() * 1000, false);
        return deferred.promise;
      } else {
    	  //console.log(results)
    	  return results;
      }
    }
    
    /* When user is typing > Only update when field is empty */
    function searchTextChange(text) {
      if(text && text.trim() === ""){
    	  $scope.$emit('relation-column-selected', text, '');
      }
    }

    /* When user selects an item from list > update */
    function selectedItemChange(item) {
    	//console.log('AutoComplete >>> Selected item Changed');
    	//console.log(item);
    	if(item && item.columnName){
    		$scope.$emit('relation-column-selected', item.columnName, item.columnType);
    	}
    }

    /**
     * Build `states` list of key/value pairs
     */
    function loadAll() {
    	//console.log('Checking available columns : ');
    	//console.log($scope.availableColumns);
    	if($scope.availableColumns && $scope.availableColumns.length > 0){
    		for (var i = 0; i < $scope.availableColumns.length; i++) {
    			$scope.availableColumns[i].value = $scope.availableColumns[i].columnName.toLowerCase();
			}
    		//console.log($scope.availableColumns);
    		return $scope.availableColumns;
    	} else {
    		return [];
    	}
     
    }

    /**
     * Create filter function for a query string
     */
    function createFilterFor(query) {
      var lowercaseQuery = angular.lowercase(query);

      return function filterFn(state) {
        return (state.value.indexOf(lowercaseQuery) === 0);
      };

    }
});


DBMigratorApp.controller('customAutoCompleteColumnCtrl', function($scope, popupFactory, $timeout, $controller){
	angular.extend(this, $controller('cleanUpControllerMixin', {$scope: $scope}));
	var self = this;
	
	self.simulateQuery = false;
	self.isDisabled    = false;
	
	// list of `state` value/display objects
	self.states        = loadAll();
	self.querySearch   = querySearch;
	self.selectedItemChange = selectedItemChange;
	self.searchTextChange   = searchTextChange;
	
	//TEMP CODE ---------------------------------
	$scope.availableItems = [
	                         {
	                        	 label: "DUM"
	                         },{
	                        	 label: "DUM2"
	                         },{
	                        	 label: "DUM3"
	                         },{
	                        	 label: "DUM4"
	                         },{
	                        	 label: "DUM5"
	                         }];
	
	//-------------------------------------
	
	
	
	//$scope.eventListeners = [];
	
	//self.newState = newState;
	
	$scope.eventListeners.push($scope.$on('upddateAutocompleteValue', function (e, newValue) {
		//console.log('DATALOADED : '+columnValue);
		self.states  = loadAll();
		if(newValue){
			self.searchText = newValue;
		} else {
			self.searchText = '';
		}
		/* Sometimes when the list doesnt refresh */
		$timeout(function () { $scope.$apply(); }, 0, false);
		//element.select2();
	}));
	
	/*function newState(state) {
      alert("Sorry! You'll need to create a Constituion for " + state + " first!");
    }*/
	
	// ******************************
	// Internal methods
	// ******************************
	
	/**
	 * Search for states... use $timeout to simulate
	 * remote dataservice call.
	 */
	function querySearch (query) {
		var results = query ? self.states.filter( createFilterFor(query) ) : self.states,
				deferred;
		if (self.simulateQuery) {
			deferred = $q.defer();
			$timeout(function () { deferred.resolve( results ); }, Math.random() * 1000, false);
			return deferred.promise;
		} else {
			//console.log(results)
			return results;
		}
	}
	
	/* When user is typing > Only update when field is empty */
	function searchTextChange(text) {
		if(text && text.trim() === ""){
			$scope.$emit('autocomplete-selected', text, '');
		}
	}
	
	/* When user selects an item from list > update */
	function selectedItemChange(item) {
		//console.log('AutoComplete >>> Selected item Changed');
		//console.log(item);
		if(item && item.columnName){
			$scope.$emit('autocomplete-selected', item.label, item.data);
		}
	}
	
	/**
	 * Build `states` list of key/value pairs
	 */
	function loadAll() {
		//console.log('Checking available columns : ');
		//console.log($scope.availableColumns);
		if($scope.autoCompleteItems && $scope.autoCompleteItems.length > 0){
			for (var i = 0; i < $scope.availableItems.length; i++) {
				if(!$scope.autoCompleteItems.value){
					$scope.availableItems[i].value = $scope.availableItems[i].label.toLowerCase();
				}
			}
			//console.log($scope.availableColumns);
			return $scope.availableItems;
		} else {
			return [];
		}
		
	}
	
	/**
	 * Create filter function for a query string
	 */
	function createFilterFor(query) {
		var lowercaseQuery = angular.lowercase(query);
		
		return function filterFn(state) {
			return (state.value.indexOf(lowercaseQuery) === 0);
		};
		
	}
});
