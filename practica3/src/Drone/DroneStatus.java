/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Drone;

/**
 * Distintos estados en los que puede estar un agente
 * 
 * @author Jose Saldaña
 */
public enum DroneStatus {
    SUBSCRIBED_TO_PLATFORM, WAITING_INIT_DATA, SUBSCRIBED_TO_WORLD, FINISHED, FREE, BUSY,BACKING_HOME, RECHARGING, NEED_RECHARGE, EXPLORING, NEED_SENSOR, WAITING_FOR_FINISH, ABOVE_TARGET, ABOVE_END
}
