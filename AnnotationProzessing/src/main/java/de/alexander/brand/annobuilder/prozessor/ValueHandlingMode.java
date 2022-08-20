package de.alexander.brand.annobuilder.prozessor;

/**
 * Enum zum Beschreiben wie mit den Werten einer Variable umgegangen werden soll.
 */
public enum ValueHandlingMode {
    /**
     * Der Wert wird immer im Erzeugen Objekt gesetzt.
     * Der Wert einer Variable wird nicht vom Builder initialisiert.
     * In der Build-Methode wird der Wert der Variable beim erzeugten Objekt gesetzt.
     */
    ALWAYS_SET,
    /**
     * Der Wert wird nur gesetzt, wenn er auch durch eine Methode des Builders gesetzt wurde.
     * Der Wert einer Variable wird nicht vom Builder initialisiert.
     * In der Build-Methode wird der Wert der Variable beim erzeugten Objekt nur gesetzt, wenn auch die With- oder Add-Methode zu diesem Argument aufgerufen wurde.
     */
    ONLY_SET_WHEN_SET,
    /**
     * Wenn der Wert nicht durch eine Methode gesetzt wurde, wird er mithilfe des ProviderStrings erzeugt.
     * Der Wert einer Variable wird vom Builder erst initialisiert, wenn sie beim Aufruf der Build-Methode nicht gesetzt wurde.
     * In der Build-Methode wird der Wert der Variable beim erzeugten Objekt gesetzt.
     */
    ONLY_PROVIDE_WHEN_NOT_SET,
    /**
     * Im Konstruktor des Builders wird die Variable initialisiert.
     * In der Build-Methode wird der Wert der Variable beim erzeugten Objekt gesetzt.
     */
    PROVIDE_ON_INIT;
}
