window.onload = function () {
    tryFindSketch();
}

function tryFindSketch () {
    var sketch = Processing.getInstanceById(getProcessingSketchId());
    if ( sketch == undefined ) return setTimeout(tryFindSketch, 200);
    
    var controller = new Controller(sketch,"form-form");
    sketch.setController(controller);
}

/**
 *    Maybe someone wants to take over and make this into a 
 *    full-fledged interface library?
 */
var Controller = (function(){
    
    function Controller () {
        var sketch = arguments[0];
        var form = document.getElementById(arguments[1]);
        form.onsubmit = function () {return false};
        var inputs = {};
        
        this.createInputElement = function ( id, type, labelStr ) {
            var input = document.createElement('input');
            input.id = id;
            input.name = id;
            input.type = type;
            if ( labelStr !== undefined && labelStr !== '' )
            {
                var label = document.createElement('label');
                label['for'] = id;
                label.id = id+'-label';
                label.innerHTML = labelStr;
                form.appendChild(label);
            }
            form.appendChild(input);
            return input;
        }
        
        this.addInputField = function ( l, t ) {
            var id = createIdFromLabel(l);
            if ( inputs[id] == undefined ) {
                inputs[id] = this.createInputElement(id, t, l);
                inputs[id].onchange = function(){
                    changeFunc()(sketch, id, this.value);
                    return false;
                }
            }
            return inputs[id];
        }
        
        this.addRange = function ( l, c, mi, mx ) {
            var input = this.addInputField( l, "range" );
            input.value = c;
            input.min = mi;
            input.max = mx;
            return input;
        }
        
        this.addPassword = function ( l ) {
            var input = this.addInputField ( l, "password" );
            return input;
        }
        
        this.addEmail = function ( l ) {
            var input = this.addInputField ( l, "email" );
            return input;
        }
        
        this.addSearch = function ( l, c ) {
            var input = this.addInputField ( l, "search" );
            input.value = c;
            return input;
        }
        
        this.addNumber = function ( l, c ) {
            var input = this.addInputField ( l, "number" );
            input.value = c;
            return input;
        }
        
        this.addTelephone = function ( l, c ) {
            var input = this.addInputField ( l, "tel" );
            input.value = c;
            return input;
        }
        
        this.addUrl = function ( l, c ) {
            var input = this.addInputField ( l, "url" );
            input.value = c;
            return input;
        }
        
        this.addDate = function ( l, c ) {
            var input = this.addInputField ( l, "date" );
            input.value = c;
            return input;
        }
        
        this.addCheckbox = function ( l, c ) {
            var id = createIdFromLabel(l);
            if ( inputs[id] == undefined ) {
                inputs[id] = this.createInputElement(id, "checkbox", l);
                inputs[id].onchange = function(){
                    changeFunc()(sketch, id, this.checked);
                    return false;
                }
            }
            inputs[id].checked = c ? 'checked' : '';
            return inputs[id];
        }
        
        this.addTextfield = function ( l, c ) {
            var id = createIdFromLabel(l);
            if ( inputs[id] == undefined ) {
                inputs[id] = this.createInputElement(id, "text", l);
                inputs[id].onchange = function(){
                    changeFunc()(sketch, id, this.value);
                    return false;
                }
            }
            inputs[id].value = c;
            return inputs[id];
        }
        
        this.addTextarea = function ( l, c ) {
            var id = createIdFromLabel(l);
            if ( inputs[id] == undefined ) {
                var label = document.createElement('label');
                label['for'] = id;
                label.id = id+'-label';
                label.innerHTML = l;
                form.appendChild(label);
                inputs[id] = document.createElement('textarea');
                inputs[id].id = id;
                inputs[id].name = id;
                inputs[id].innerHTML = c;
                inputs[id].onchange = function(){
                    changeFunc()(sketch, id, this.value);
                    return false;
                }
                form.appendChild(inputs[id]);
            }
            inputs[id].value = c;
            return inputs[id];
        }
        
        this.addSelection = function ( l, o ) {
            var id = createIdFromLabel(l);
            if ( inputs[id] == undefined ) {
                var label = document.createElement('label');
                label['for'] = id;
                label.id = id+'-label';
                label.innerHTML = l;
                form.appendChild(label);
                var select = document.createElement('select');
                select.id = id;
                select.name = id;
                if ( o !== undefined && o.length && o.length > 0 ) {
                    for ( var i = 0; i < o.length; i++ ) {
                        var value = o[i].length > 1 ? o[i][1] : i;
                        var option = document.createElement('option');
                        option.innerHTML = o[i][0];
                        option.value = value;
                        select.appendChild(option);
                    }
                }
                select.onchange = function( event ){
                    changeFunc()(sketch, id, this.value);
                    return false;
                }
                inputs[id] = select;
                form.appendChild(inputs[id]);
            }
            return inputs[id];
        }
        this.addMenu = this.addSelection;
        
        this.setElementLabel = function ( element, labelStr ) {
            var label = document.getElementById(element.id+'-label');
            if ( label && label.childNodes && label.childNodes.length > 0 ) {
                label.childNodes[0].textContent = labelStr;
            } else {
                //console.log([element, label]);
            }
        }
    }
    
    var changeFunc = function () {
        return function ( sketch, id, value ) {
            try {
                sketch[id](value);
            } catch (e) {
                //console.log(e);
                sketch.println( "Function \"void "+id+"(value)\" is not defined in your sketch.");
            }
        }
    }
    
    var createIdFromLabel = function ( l ) {
        return l.replace(/^[^-_a-z]/i,'_').replace(/[^-_a-z0-9]/gi,'');
    }
        
    return Controller;

})();
