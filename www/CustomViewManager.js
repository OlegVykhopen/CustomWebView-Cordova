    /*
     * Custom plugin for native views
     *
     * @ created by Oleg Vykhopen
     *
     * */

    var exec = require("cordova/exec");

    function propsToString(obj) {
        // stringify all vars
        for (var i in obj) {
            if (obj.hasOwnProperty(i)) {
                obj[i] = '' + obj[i];
            }
        }
    }

    var CustomViewManager = function (name) {
        this.name = name;
        this.views = {};
    };

// View for Cordova
    var View = function (name) {
        this.name = name;
    }

    View.prototype.postMessage = function (message) {
        cordova.exec(null, null, "CustomViewManagerPlugin", "postMessage", [message, this.name]);
    };

    View.create = function (name, options, success, failure) {
        propsToString(options);
        cordova.exec(success, failure, "CustomViewManagerPlugin", "createView", [name, options]);
    };

    View.prototype.remove = function (success, failure) {
        cordova.exec(success, failure, "CustomViewManagerPlugin", "removeView", [this.name]);
    };

    View.prototype.show = function (animOptions, success, failure) {
        cordova.exec(success, failure, "CustomViewManagerPlugin", "showView", [this.name, animOptions]);
    };

    View.prototype.hide = function (animOptions, success, failure) {
        cordova.exec(success, failure, "CustomViewManagerPlugin", "hideView", [this.name, animOptions]);
    };

    View.prototype.load = function (source, success, failure) {
        cordova.exec(success, failure, "CustomViewManagerPlugin", "load", [this.name, { src: source }]);
    };

    View.prototype.setLayout = function (options, success, failure) {
        propsToString(options);
        cordova.exec(success, failure, "CustomViewManagerPlugin", "setLayout", [this.name, options]);
    };

    CustomViewManager.prototype.receivedMessage = function (message, senderName) {
        // for more information on the MessageEvent API, see:
        // http://www.w3.org/TR/2008/WD-html5-20080610/comms.html

        var sender = this.views[senderName];

        var event = document.createEvent('MessageEvent');
        event.initMessageEvent('typeArg???', true, true, message, senderName, '', sender);
        window.dispatchEvent(event);
    };


    CustomViewManager.prototype.throwError = function (cb, error) {
        if (cb) {
            cb(error);
        } else {
            throw error;
        }
    };

    CustomViewManager.prototype.create = function (name, options, success, failure) {
        if (!View.create) {
            return this.throwError(failure, new Error('The create API is not implemented, while trying to create: ' + name));
        }

        var views = this.views;
        // wrap around the success callback, so we can return a View instance

        function successWrapper() {
            var view = new View(name);

            views[name] = view;

            success(view);
        }

        View.create(name, options, successWrapper, failure);
    };

    CustomViewManager.prototype.show = function (name, animOptions, success, failure) {
        if (!this.views[name]) {
            return this.throwError(failure, new Error('Show Error with view name: ' + name + '. View does not exist'));
        }

        this.views[name].show(animOptions, success, failure);
    };

    CustomViewManager.prototype.hide = function (name, animOptions, success, failure) {
        if (!this.views[name]) {
            return this.throwError(failure, new Error('Hide Error with view name: ' + name + '. View does not exist'));
        }

        this.views[name].hide(animOptions, success, failure);
    };

    CustomViewManager.prototype.remove = function (name, success, failure) {
        if (!this.views[name]) {
            return this.throwError(failure, new Error('Hide Error with view name: ' + name + '. View does not exist'));
        }

        this.views[name].remove(success, failure);
    };

    CustomViewManager.prototype.setLayout = function (name, animOptions, success, failure) {
        if (!this.views[name]) {
            return this.throwError(failure, new Error('Set Layout Error with view name: ' + name + '. View does not exist'));
        }

        this.views[name].setLayout(animOptions, success, failure);
    };

    CustomViewManager.prototype.load = function (name, source, success, failure) {
        if (!this.views[name]) {
            return this.throwError(failure, new Error('Load Error with view name: ' + name + '. View does not exist'));
        }

        this.views[name].load(source, success, failure);
    };

    CustomViewManager.prototype.updateViewList = function (list) {

        // check for removed views
        for (var name in this.views) {
            if (list.indexOf(name) === -1) {
                delete this.views[name];
            }
        }

        // check for new views
        for (var i = 0; i < list.length; i++) {
            var name = list[i];

            if (!this.views[name]) {
                this.views[name] = new View(name);
            }
        }

    };

// instantiate the CustomViewManager (always named "mainView" in Cordova)
    window.CustomViewManager = new CustomViewManager();
    module.exports = CustomViewManager;

