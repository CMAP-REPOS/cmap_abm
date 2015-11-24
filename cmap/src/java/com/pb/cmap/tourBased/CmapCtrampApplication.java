 package com.pb.cmap.tourBased;

import java.util.ResourceBundle;


import com.pb.common.util.ResourceUtil;
import com.pb.models.ctrampIf.jppf.CtrampApplication;


 public class CmapCtrampApplication extends CtrampApplication{

     public static final String PROGRAM_VERSION = "09June2008";
     public static final String PROPERTIES_PROJECT_DIRECTORY = "Project.Directory";



     public CmapCtrampApplication( ResourceBundle rb ){
         super( rb );

         projectDirectory = ResourceUtil.getProperty(rb, PROPERTIES_PROJECT_DIRECTORY);

     }

 }