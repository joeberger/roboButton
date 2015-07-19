base_thickness = 2;
standoff_height = 8;
base_width = 130;
base_height = 130;
bottom_wall_height = 30;
wall_thickness = 2;
top_offset = -2;
//top_offset = 10;

$fn=20;


module bottom() {
    color("Green") {
        translate([-(base_width/2),-(base_height/2),0]) {
            cube([base_width, base_height, base_thickness]);
        }
    }
}

module arduino_standoffs() {
    // relay standoffs
    color("Yellow") {
        translate([10, 53, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([15, 0, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([43, 0, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([58, 50, 0]) {
            cylinder(h=standoff_height, r=4);
        }        
    }
    color("Orange") {
        translate([10, 53, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([15, 0, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([43, 0, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([58, 50, standoff_height]) {
            cylinder(h=4, r=1);
        }        
    }
} 

module relay_standoffs() {
    color("Red") {
        translate([0, 0, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([0, 28, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([22, 0, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([22, 28, 0]) {
            cylinder(h=standoff_height, r=4);
        }        
    }
    color("Purple") {
        translate([0, 0, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([0, 28, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([22, 0, standoff_height]) {
            cylinder(h=4, r=1);
        }
        translate([22, 28, standoff_height]) {
            cylinder(h=4, r=1);
        }        
    }
}    
  

module transformer_standoffs() {
    color("Red") {
        translate([0, 0, 0]) {
            cylinder(h=standoff_height, r=4);
        }
        translate([-7, 54.5, 0]) {
            cylinder(h=standoff_height, r=4);
        }   
        translate([7, 54.5, 0]) {
            cylinder(h=standoff_height, r=4);
        }  
    }
    color("Purple") {
        translate([0, 0, standoff_height]) {
            cylinder(h=4, r=1.5);
        }
        difference() {
            translate([7, 54.5, standoff_height]) {
                cylinder(h=4, r=2);
            }   
            translate([5, 50.5, standoff_height]) {
                cube([4,4,8]);
            }  
        }   
        difference() {
            translate([-7, 54.5, standoff_height]) {
                cylinder(h=4, r=2);
            }   
            translate([-9, 50.5, standoff_height]) {
                cube([4,4,8]);
            }  
        }        
    } 
}    

module wall(height) {
    color("Blue") {
        cube([base_width, wall_thickness, height]);
    }
}

module duct() {
    rotate([90,0,0]) {
        color("Red") {
            cylinder(h=10, r=10);
        }
        translate([-10, -18, 0]) {
            // width, height, depth
            cube([20, 15, 10]);
        }
    }
}

module right_wall() {
    difference() {
        union() {
            translate([-base_height/2, base_height/2-wall_thickness, base_thickness]) {
                wall(bottom_wall_height);
            }
            translate([8, base_width/2+wall_thickness-4, 20]) {
                duct();
            }
        }
        union() {
            translate([8,0,0]) {
                translate([0, base_width/2+wall_thickness, 24]) {
                    rotate([90,0,0]) {
                        color("Purple") {
                            cylinder(h=25, r=9);
                        }
                    }
                }
                translate([-10, base_width/2+wall_thickness-4, 17]) {
                    rotate([90,0,0]) {
                        color("Yellow") {
                            cube([20, 20, 23]);
                        }
                    }
                }  
                translate([-12, base_width/2+wall_thickness-9, 10]) {
                    rotate([0,90,0]) {
                        color("Green") {
                            cylinder(h=30, r=3);
                        }
                    }
                }   
            }
        }     
    }
}

module screw_mount() {
    translate([base_width/2-4, base_width/2-4, base_thickness]) {
        color("Blue") {
            rotate([0, 0, 90]) {
                cylinder(h=bottom_wall_height, r=4);
            }
        }
    }
}

module serial_plug_mount() {
    // external serial connection
    translate([-55, -65, base_thickness]) {
        rotate([0, 0, 90]) {
            cube([10, 10, 3]);
        }
    } 
    translate([-35, -65, base_thickness]) {
        rotate([0, 0, 90]) {
            cube([10, 10, 3]);
        }
    } 
    translate([-35, -65, base_thickness+3]) {
        rotate([0, 0, 90]) {
            cube([7, 30, 3]);
        }
    } 
    translate([-45, -55, base_thickness]) {
        color("Red") {
            rotate([0, 0, 90]) {
                cube([3, 10, 1]);
            }
        }
    }     
}

module corner_bevel() {
    translate([base_width/2+2, base_height/2-6, -5]) {
        rotate([0, 0, 45]) {
            cube([5, 20, base_thickness+bottom_wall_height*5]);
        }
    }
}

module top_bezel_edges() {
    // bezel
    translate([base_width/2, base_width/2-wall_thickness-.25, bottom_wall_height + top_offset]) {
        rotate([0, 0, 90]) {
            cube([wall_thickness*3,base_width,base_thickness*2]);
        }
    }
    rotate([0,0,90]) {
        translate([base_width/2, -base_width/2-wall_thickness+.25, bottom_wall_height + top_offset]) {
            rotate([0, 0, 90]) {
                cube([wall_thickness*2,base_width,base_thickness*2]);
            }
        }   
    }
}

module top_bezel_total() {
    // screw mount offsets
    translate([base_width/2-4, base_width/2-4, bottom_wall_height + top_offset]) {
        rotate([0, 0, 90]) {
            cylinder(h=base_thickness*2, r=4.5);
        }
    }
    top_bezel_edges();    
}

module top() {
    union() {
        // transformer push-down
        color("Orange") {
            translate([39-7, -49-5, top_offset + standoff_height + 10]) {
                cylinder(h=bottom_wall_height-16 + 4 - standoff_height + 8, r=4);
            }
        }
        
        // relay push-down
        color("Orange") {
            translate([50, 47-8, top_offset + standoff_height + 10]) {
                cylinder(h=bottom_wall_height-16 + 4 - standoff_height + 8, r=4);
            }
        }
        
        // SerialPort push-down
        translate([-42-8, -30-18, top_offset + standoff_height + 10-12]) {
            cylinder(h=bottom_wall_height-16 + 12 - standoff_height + 8, r=4);
        }
      
        difference() {
            color("Purple") {
                translate([-(base_width/2),-(base_height/2),bottom_wall_height + 2 + top_offset]) {
                    cube([base_width, base_height, base_thickness*3]);
                }
            }    
      
            color("Red") {
                translate([-(base_width/2-9),-(base_height/2-9),bottom_wall_height + 2 + top_offset]) {
                    cube([base_width-18, base_height-17, base_thickness*2]);
                }
            }   
            
            top_bezel_total();
            mirror([0,1,0]) {
                top_bezel_total();
            }
            mirror([1,0,0]) {
                top_bezel_total();
                mirror([0,1,0]) {
                    top_bezel_total();
                }
            }   
     
            corner_bevel();
            mirror([0,1,0]) {
                corner_bevel();
            }
            mirror([1,0,0]) {
                corner_bevel();
                mirror([0,1,0]) {
                    corner_bevel();
                }
            }       
        }
    }
}

 
 
    

//////////////////////////////////////////////////////////////
// START RENDERING
//////////////////////////////////////////////////////////////

module topAndBottom() {
    difference() {
        union() {
            right_wall();
            mirror([0,1,0]) {
                right_wall();
            }
            // back
            translate([-base_height/2+2, -base_height/2, base_thickness]) {
                rotate([0,0,90]) {
                    wall(bottom_wall_height);
                }
            }
            
            // front
            translate([base_height/2, -base_height/2, base_thickness]) {
                rotate([0,0,90]) {
                    wall(bottom_wall_height);
                }
            }
            bottom();
            
            serial_plug_mount();
            
            translate([28, 19, base_thickness]) {
                relay_standoffs();
            }
            translate([39, -49, base_thickness]) {
                transformer_standoffs();
            }
            translate([-57, -30, base_thickness]) {
                arduino_standoffs();
            }
            
            screw_mount();
            mirror([0,1,0]) {
                screw_mount();
            }
            mirror([1,0,0]) {
                screw_mount();
                mirror([0,1,0]) {
                    screw_mount();
                }
            }   
            
            top();
            
        }
       
        // wall holes
        translate([-25, -60, 25]) {
            rotate([90, 0, 0]) {
                cylinder(h=10, r=4);
            }
        }
        translate([-25, -60, 21]) {
            rotate([90, 0, 0]) {
                cylinder(h=10, r=3);
            }
        }
        translate([-40, -60, 21]) {
            rotate([90, 0, 0]) {
                cylinder(h=10, r=4);
            }
        }
        translate([-45, -70, base_thickness]) {
            rotate([0, 0, 90]) {
                cube([20, 10, 3]);
            }
        } 
    
        corner_bevel();
        mirror([0,1,0]) {
            corner_bevel();
        }
        mirror([1,0,0]) {
            corner_bevel();
            mirror([0,1,0]) {
                corner_bevel();
            }
        }
        
        translate([-13, base_width/2+wall_thickness, base_thickness+standoff_height+8]) {
            rotate([90,0,0]) {
                color("Red") {
                    cylinder(h=25, r=9);
                }
            }
        }     
    }
}

rotate([0,180,0]) {
    top();
}
//topAndBottom();

    



 





