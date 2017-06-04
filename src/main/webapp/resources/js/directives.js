DBMigratorApp.directive('droppableArea', function(){
	return {
		restrict: 'A',
		link: function(scope,element,attrs){
			element.on("dragover", function(eventObject){
				/* NOTE : dataTransfer.getData cannot be accessed in any event except "drop" as per specification of Chrome and IE. */
				//console.log(eventObject.originalEvent.dataTransfer.getData("text"));
				eventObject.preventDefault(); // Allow to drop
            });
 
            element.on("drop", function(eventObject) {
            	if(attrs.callbackfn){
            		scope[attrs.callbackfn](eventObject);
            	} else {
            		console.log('Directive : droppableArea : Error : No Callback function defined on Drop event')
            	}
                eventObject.preventDefault();
            });
		}
	}
});


DBMigratorApp.directive('sortableDropArea', ['popupFactory', function(popupFactory){
	return {
		restrict: 'A',
		link: function(scope,element,attrs){
			element.on("dragover", function(eventObject){
				element.addClass('sortable-drag-over');
				eventObject.preventDefault(); // Allow to drop
			});
			
			element.on("dragleave", function(eventObject){
				element.removeClass('sortable-drag-over');
				//eventObject.preventDefault(); // Allow to drop
			});
			
			element.on("drop", function(eventObject) {
				element.removeClass('sortable-drag-over');
				if(attrs.callbackfn){
					scope[attrs.callbackfn](eventObject);
				} else {
					console.log('Directive : sortableDropArea : Error : No Callback function defined on Drop event')
				}
				/*var xferData = (JSON.parse(eventObject.originalEvent.dataTransfer.getData("text")));
				if(scope.nodeGroup.nodeName == xferData.nodeName){
					if(scope.data.name != xferData.tableName){
						// Put the dragged element before current element in array
						if(attrs.callbackfn){
							scope[attrs.callbackfn](eventObject);
						} else {
							console.log('Directive : sortableDropArea : Error : No Callback function defined on Drop event')
						}
					}
				} else {
					popupFactory.showInfoPopup('Access Denied!!', 'Only tables from same node can be rearranged !!.', false);
				}*/
				eventObject.preventDefault();
			});
		}
	}
}]);

DBMigratorApp.directive('draggableElement', function(){
	return {
		restrict: 'A',
		link: function(scope,element,attrs){
			element.on('dragstart', function(ev){
				if(attrs.xferData){
					ev.originalEvent.dataTransfer.setData("text", attrs.xferData);
				}
			});
		}
	}
});

DBMigratorApp.directive('draggableText', function(){
	return {
		restrict: 'E',
		template: '<span draggable="true" class="draggable-text-element"></span>',
		replace: true,
		link: function(scope,element,attrs){
			element.on('dragstart', function(ev){
				
				/* NOTE : IE and CHROME only support "text" attribute while saving data in setData */
				if(attrs.xferData){
					ev.originalEvent.dataTransfer.setData("text", attrs.xferData);
				} else {
					ev.originalEvent.dataTransfer.setData("text", ev.target.textContent);
				}
			});
			
			element.on('dragend', function(ev){
				//scope.getColumns({name: element.textContent});
			})
			
			/* bind changes */
			attrs.$observe('elemLabel', function(val){
				element.text(val)
			})
		}
	}
});