The code builds upon the COM3505 excercise sheets, I did not create the graphical assets used. 
Any code that does not appear in the excercise sheet is my own. 

---------RUNNING THE PROGRAM-------
Navigate to the assignment folder and execute main.bat. Alternatively, compile all java files and run 
the Main class. JOGL must be setup beforehand.

--------------THE SNOWMAN----------
Produced using a hierarchical model, the head of Frosty (the snowman) has been given a slight vertical 
offset, so that it slightly overlaps with the body. I felt this looked a little more believable than 
having the head perfectly sit on the body. The same principle is applied to that hat. 
That hat itself includes the letter F at the front of the model. 

The eyes and buttons of the snowman are made of a rough stone texture, and so have no specular 
component. The nose and mouth are made of a smooth stone texture, and so have a large specular 
component. 

The interface buttons can be used to animate the snowman. The animation will start slow, and
gradually increase in speed up to a maximum. The roll effect will roll Frosty's head to the left, 
before also applying a front / back roll. This roll will then continue in a circle. 
When selecting a different animation, the current animation will first slow down, and Frosty will be 
returned to his starting position before beginning the new animation. When stopping an animation, 
the animation will continue for a short time as the animation speed slows down. The reset button 
can be used to stop all animations.

--------------THE BACKGROUND-------
Animated with a snow texture, the speed of the snow will periodically change, giving the impression 
of a changing wind speed. This change will be gradual, and will speed up and slow down smothly.
The background can be lit with both the main light and the spotlight, and has no specular component.

--------------THE LIGHTS-----------
The main light can be increased or decreased, and sits above the scene, representing the sun. 
The spotlight is more yellow in colour, and sits above a rotating pole. The spotlight can be toggled 
on and off, which will also toggle the rotation of the pole. The main light can also be varied in 
brightess. The models for the lights themselves will decrease in brightness as the emitted light does. 

--------------THE SPECULAR OBJECT-----------
Beside the snowman is a crate with an applied specular map. This is propped up by a smaller crate so
that it better catches the light from the spotlight. The specular highlights can easily be seen from 
the camera's starting position.