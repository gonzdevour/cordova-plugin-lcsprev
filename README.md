# Cordova Plugin LCR

Simple plugin that returns LCR result

## Using

Create a new Cordova Project

    $ cordova create testlcr com.example.testlcr testLCR
    
Install the plugin

    $ cd testlcr
    copy iOS lcr plugin into ./plugins
    $ cordova plugin add ./plugins/lcr
    

Edit `www/js/index.js` and add the following code inside `onDeviceReady`

```js
    var success = function(message) {
        alert(message);
    }

    var failure = function() {
        alert("Error calling Hello Plugin");
    }

    lcr.scan(success, failure);
```

Install iOS or Android platform

    cordova platform add ios
    cordova platform add android
    
Run the code

    cordova run 

## More Info

For more information on setting up Cordova see [the documentation](http://cordova.apache.org/docs/en/latest/guide/cli/index.html)

For more info on plugins see the [Plugin Development Guide](http://cordova.apache.org/docs/en/latest/guide/hybrid/plugins/index.html)
