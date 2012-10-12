function log(m,s){
	process.stdout.writeln("module."+m+": "+s);
}
function argv(args){
	return Array.prototype.slice.call(args).join(",");
}

exports._nodeModulePaths=function(){
	process.stdout.writeln("_nodeModulePaths",argv(arguments));
};