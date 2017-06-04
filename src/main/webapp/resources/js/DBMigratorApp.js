var DBMigratorApp = angular.module('DBMigratorApp', [
'ngRoute', 'smart-table', 'ngMaterial', 'ngAnimate', 'ui.bootstrap']);

DBMigratorApp.config(['$routeProvider', '$locationProvider',
    function($routeProvider, $locationProvider) {
         $routeProvider.
         	when('/', {
	        	 isUser: function(user){
	        		 if(user){
	        			 return true;
	        		 } else {
	        			 return false;
	        		 }
	        	 }
     		}).when('/SelectDB', {
                 templateUrl: 'resources/templates/selectDBViewTemplates.html',
                 controller: 'dbSelectionController'
             }).when('/Migrate', {
                 templateUrl: 'resources/templates/migrateViewTemplates.html',
                 authDB: function (user) {
                     return user && user.NewData
                   }
             }).when('/MigrateRev', {
                 templateUrl: 'resources/templates/migrateViewReverseTemplates.html',
                 authDB: function (user) {
                     return user && user.NewData
                   }
             }).when('/AdminPage', {
                 templateUrl: 'resources/templates/adminPageTemplates.html',
                 isAdmin: function (user) {
                     return user && (user.userRole == "Admin")
                   }
             }).when('/Home', {
                 templateUrl: 'resources/templates/homeViewTemplates.html'
             }).otherwise({
             }).when('/DBManager', {
            	 templateUrl: 'resources/templates/DBAssignManagerTemplates.html'
             }).otherwise({
            	 template: '<h3>Invalid URL :/ </h3>'
             });
        // $locationProvider.html5Mode(false).hashPrefix('!');
    }
]).run(function ($rootScope, $location) {
	$rootScope.$on('$routeChangeStart', function (ev, next, curr) {
		if(!next){
			$location.path('/Home') ;
		}
		else if(next.$$route) {
			if(next.$$route.isUser){
				var user = jsUserData;
				if(next.$$route.isUser(user)){
					$location.path('/Home') ;
				}
			}
			if (next.$$route.authDB) { // On Migrate Page check if selections has been done. if not, redirect to DB selection page.
				var user = $rootScope.UserData
				var auth = next.$$route.authDB
				if (auth && !auth(user)) { 
					$location.path('/SelectDB') ;
				}
		    }
			
			if (next.$$route.isAdmin) { // On Migrate Page check if selections has been done. if not, redirect to DB selection page.
				var user = $rootScope.UserData
				var auth = next.$$route.isAdmin
				if (auth && !auth(user)) { 
					$location.path('/Home') ;
				}
		    }
		}
  	})
});
