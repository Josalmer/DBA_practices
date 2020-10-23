/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practica2;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.util.ArrayList;

/**
 *
 * @author jose
 */
public class Knowledge {
    Integer currentPositionX;
    Integer currentPositionY;
    Integer currentHeight;
    Integer energy;
    Integer orientation;
    Integer mapWidth;
    Integer mapHeight;
    Integer maxFlight;
    Double angular;
    Double distanceToObjective;
    Integer nActionsExecuted;
    ArrayList<Integer> orientations;
    
    ArrayList<ArrayList<Integer>> map;
    
    /**
     * @author Jose Saldaña, Manuel Pancorbo
     * @param answer 
     */
    public void initializeKnowledge(JsonObject answer) {
        this.energy = 1000;
        this.nActionsExecuted = 0;
        this.mapWidth = answer.get("width").asInt();
        this.mapHeight = answer.get("height").asInt();
        this.maxFlight = answer.get("maxflight").asInt();
        this.initializeMap();
    }
    
    /**
     * @author Jose Saldaña
     * @author Manuel Pancorbo
     */
    public void initializeMap() {
        this.map = new ArrayList<>();
        for (int i = 0; i < this.mapWidth; i++) {
            ArrayList<Integer> row = new ArrayList<>();
            for (int j = 0; j < this.mapHeight; j++) {
                row.add(-1);
            }
            this.map.add(row);
        }
    }
    
    /**
     * @author Manuel Pancorbo
     */
    public void initializePossibleOrientations() {
        this.orientations = new ArrayList<>();

        // -45 0 45 90 135 180 -135 -90
        this.orientations.add(-45);
        this.orientations.add(0);
        this.orientations.add(45);
        this.orientations.add(90);
        this.orientations.add(135);
        this.orientations.add(180);
        this.orientations.add(-135);
        this.orientations.add(-90);
    }

    /**
     * @author Manuel Pancorbo
     * @param wantedOrientation
     * @return how many turns are to be made
     */
    public int howManyTurns(int wantedOrientation) {
        int turns = 0;
        int actual = this.orientations.indexOf(this.orientation);
        int next = (actual + turns) % this.orientations.size();
        while (this.orientations.get(next) != wantedOrientation) {
            turns++;
            next = (actual + turns) % this.orientations.size();
        }

        return turns;
    }    

    /**
     * @author Manuel Pancorbo
     * @param turns how many turns does agent have to do in order to get to his
     *              wanted orientation
     * @return 0 if agent should turn left, 1 if agent should turn right
     */
    public boolean shouldTurnRight(int turns) {
        boolean rightTurn;
        if (turns > orientations.size() / 2)
            rightTurn = false;
        else
            rightTurn = true;

        return rightTurn;
    }
    
    /**
     * @author Jose Saldaña
     * @return current FlootHeight
     */
    public int getFloorHeight() {
        return this.map.get(this.currentPositionX).get(this.currentPositionY);
    }

    /**
     * @author Domingo Lopez
     */
    public void moveForward(){
        int ABScurrentOrientation = Math.abs(this.orientation); //Valor absoluto de la orientación
        
        //Posición X
        if(ABScurrentOrientation != 0 || ABScurrentOrientation != 180)
            this.currentPositionX += this.orientation/ABScurrentOrientation;

        //Posición Y
        if(ABScurrentOrientation != 90)
            if(ABScurrentOrientation == 45 || ABScurrentOrientation == 0)
                this.currentPositionY -= 1;
            else
                this.currentPositionY += 1;
    }

    /**
     * @author Manuel Pancorbo
     * @param rightTurn 1 if its a right turn, 0 if its a left turn
     * @return where is the agent looking after doing that turn
     */
    public int getNextOrientation(int actualOrientation, boolean rightTurn) {
        int turn = rightTurn ? 1 : -1;
        int actual = this.orientations.indexOf(actualOrientation);
        int next = actual + turn % this.orientations.size();

        return this.orientations.get(next);
    }
    
    /**
     * @author Domingo Lopez
     * @param action
     * @return energyCost of an action
     */
    public int energyCost(AgentAction action, int nSensors) {
        int energy = 0;
        switch (action) {
            case moveF: energy = 1; break;
            case moveUp: energy = 5; break;
            case moveD: energy = 5; break;
            case rotateL: energy = 1; break;
            case rotateR: energy = 1; break;
            case touchD: energy = this.getFloorHeight(); break;
            case LECTURA_SENSORES: energy = nSensors; break;
        }
        return energy;
    }

    /**
     * @author Manuel Pancorbo
     * @author Domingo Lopez
     * @param wantedMovement
     */
    public void manageMovement(AgentAction nextMovement) {
        switch (nextMovement) {
            case moveF:
                this.moveForward();
                break;
            case moveUp:
                this.currentHeight += 5;
                break;
            case moveD:
                this.currentHeight -= 5;
                break;
            case rotateL:
                this.orientation = getNextOrientation(this.orientation, false);
                break;
            case rotateR:
                this.orientation = getNextOrientation(this.orientation, true);
                break;
            case touchD:
                this.currentHeight = this.getFloorHeight();
                break;
        }
    }
}
