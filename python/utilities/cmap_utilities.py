import yaml
import os

class CMapUtilities():
    __MODELLER_NAMESPACE__ = "cmap"
    tool_run_msg = ""
    
    def __init__(self):
        pass

    def __call__(self, parms):
        pass
    
    def readYaml(self, file):
        with open(file, "r") as stream:
            try:
                return(yaml.load(stream, Loader=yaml.SafeLoader))
            except yaml.YAMLError as exc:
                print(exc)
            