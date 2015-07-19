plateThickness=4;
plateLength=56;
plateWidth=26;

doorThickness=45;
doorHeight=110;
doorWidth=95;

cylinderBoreOffset=60;
cylinderBoreDiameter=54;

deadBoltDiameter=25;
    
electronicsBoreHeight=55;
electronicsBoreDepth=35;

module middleDoorBlock() {
   
    difference() {
        
        // Door Body
        cube([doorHeight, doorWidth, doorThickness]);
        
        // Main Lock Components
        translate([13, 0, 0]) {
            // Lock Cylinder Bore
            translate([doorHeight/2, cylinderBoreOffset, -5]) {
                cylinder(h = cylinderBoreOffset, d=cylinderBoreDiameter);
            }
            
            // DeadBolt Bore
            translate([doorHeight/2, cylinderBoreOffset/2 + plateThickness*2, doorThickness/2]) {
                rotate([90, 0, 0]) {
                    cylinder(h = cylinderBoreOffset, d=deadBoltDiameter);
                }
            }
      
            // Plate Inset
            translate([doorHeight/2 - plateLength/2, plateThickness, doorThickness/2-plateWidth/2]) {
                rotate([90, 0, 0]) {
                    cube([plateLength, plateWidth, plateThickness]);
                }
            } 
            // Plate Scree Hole1
            translate([doorHeight/2-20, 10, doorThickness/2]) {
                rotate([90, 0, 0]) {
                    cylinder(h = 70, d=2);
                }
            }
            // Plate Scree Hole2
            translate([doorHeight/2+20, 10, doorThickness/2]) {
                rotate([90, 0, 0]) {
                    cylinder(h = 70, d=2);
                }
            }
        }
            
        // Electronics Bore
        translate([-25,((doorWidth - cylinderBoreDiameter)/2)+13, 12]) {
            cube([electronicsBoreHeight, cylinderBoreDiameter, electronicsBoreDepth]);
        }  

        // Cylinder Light Passthrough
        translate([20, doorWidth/2+12, doorThickness/2]) {
            rotate([0, 90, 0]) {
                cylinder(h=50, d=15);
            }
        }

        // Stand Bore 1
        translate([doorHeight-10, doorWidth*.25, doorThickness/2]) {
            rotate([0, 90, 0]) {
                cylinder(h=10, d=10);
            }
        }
        
        // Stand Bore 2
        translate([doorHeight-10, doorWidth*.76, doorThickness/2]) {
            rotate([0, 90, 0]) {
                cylinder(h=10, d=10);
            }
        }    
   
        // Top Bore 1
        translate([-2, doorWidth*.1, doorThickness/2]) {
            rotate([0, 90, 0]) {
                cylinder(h=10, d=7);
            }
        }
        
        // Top Bore 2
        translate([-2, doorWidth*.96, doorThickness/2]) {
            rotate([0, 90, 0]) {
                cylinder(h=10, d=5);
            }
        }       
    }
}

module topDoorBlock() {
    
    topHeight = doorHeight/2 + 5;

    difference() {
        
        // Door Body
        cube([topHeight, doorWidth, doorThickness]);
            
        // Electronics Bore
        translate([topHeight/2-20,((doorWidth - cylinderBoreDiameter)/2)+13, 9]) {
            cube([electronicsBoreHeight, cylinderBoreDiameter, electronicsBoreDepth+10]);
        }       
    }
       
    // Top Bore 1
    translate([topHeight, doorWidth*.1, doorThickness/2]) {
        rotate([0, 90, 0]) {
            cylinder(h=5, d=6);
        }
    }
    
    // Top Bore 2
    translate([topHeight, doorWidth*.96, doorThickness/2]) {
        rotate([0, 90, 0]) {
            cylinder(h=5, d=4);
        }
    }   
}

module bottomDoorBlock() {
            
    // Door Body
    cube([10, doorWidth, doorThickness]);
        
    // Stand Bore 1
    translate([-5, doorWidth*.25, doorThickness/2]) {
        rotate([0, 90, 0]) {
            cylinder(h=5, d=9);
        }
    }
    
    // Stand Bore 2
    translate([-5, doorWidth*.76, doorThickness/2]) {
        rotate([0, 90, 0]) {
            cylinder(h=5, d=9);
        }
    } 
    
     // Base
     translate([10, -10, 55]) {
        rotate([0, 90, 0]) {
            cube([doorThickness+20, doorWidth+20, 10]);   
        }
    }   
    
}


$fn=40;
middleDoorBlock();
translate([-90, 0, 0]) {
    topDoorBlock();
}

translate([doorHeight + 30, 0, 0]) {
    bottomDoorBlock();
}

