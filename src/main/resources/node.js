var process = {};
(function(){
	function print(writer,o){
		writer.print(o||"");
	}
	function println(writer,o){
		writer.println(o||"");
	}
	function platform(){
		var prop = java.lang.System.getProperty;
		return prop("os.name") + " - " + prop("os.version")+ " ("+prop("os.version")+")";
	}
	function exit(status){
		status = status || 0;
		throw new com.ppedregal.typescript.maven.ProcessExit(status);
	}
	process = {
		stdout: {
			write:function(o){
				print(java.lang.System.out,o);
			},
			writeln:function(o){
				println(java.lang.System.out,o);
			}
		},
		stderr: {
			write:function(o){
				print(java.lang.System.err,o);
			},
			writeln:function(o){
				println(java.lang.System.err,o);
			}
		},
		platform: platform(),
		argv:[],
		exit: exit,
		mainModule: {
			filename:""
		}
	};	
})();
var console = {};
(function(){
	function doLog(){
		return Array.prototype.slice.call(arguments).join(",");
	}
	function logMsg(lvl,msg){
		return "["+lvl+"] "+Array.prototype.slice.call(msg);
	}
	console = {
		log:function(){
			process.stdout.writeln(Array.prototype.slice.call(arguments).join(","));
		},
		info:function(){
			this.log(logMsg("info",arguments));
		},
		warn:function(){
			this.log(logMsg("warn",arguments));
		},
		error:function(){
			this.log(logMsg("error",arguments));
		},
		debug:function(){
			this.log(logMsg("debug",arguments));
		},	
		trace:function(){
			this.log(logMsg("trace",arguments));
		}	
	};
})();