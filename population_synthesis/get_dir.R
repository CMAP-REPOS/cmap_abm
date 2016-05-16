# When this script is called with "source()", it will save its parent directory
# (i.e. the root population synthesis directory) to a variable called PopSynDir
# that can subsequently be used to define other paths in a relative manner.
PopSynDir <- normalizePath(dirname(parent.frame(2)$ofile))