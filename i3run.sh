#!/bin/bash

# Compile and start the application in the background
javac -d bin -cp "../../Java_libs/*" $(find src -name "*.java")
cp -r src/resources bin/
java -cp "../../Java_libs/*:bin" com.rebuild.RotatingCube &


# Wait for windows to appear
sleep 3

# Make the Control Panel floating and move both windows to workspace 5
i3-msg "[title=\"Simulation Controls\"] floating enable, move to workspace 5"
i3-msg "[title=\"Rotating Cube Demo\"] move to workspace 5"

# Focus workspace 5
i3-msg "workspace 5"
