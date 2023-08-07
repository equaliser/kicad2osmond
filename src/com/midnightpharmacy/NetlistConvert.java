package com.midnightpharmacy;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class NetlistConvert {

    private static final int VALUE_POS = 6;
    private static final int FOOTPRINT_POS = 3;
    private static final int REFERENCE_POS = 5;

    private static ArrayList<Component> components = new ArrayList<Component>();

    public static void main(String[] args ) throws java.io.IOException {

        Path pathToFile = Paths.get(args[0]);
        String outputFilename = pathToFile + ".osmond";

        for (String line : Files.readAllLines(pathToFile)) {
            // have we found a new component?
            if(line.startsWith(" ( /")) {
                Component newComponent = buildComponent(line);
                components.add(newComponent);
            }

            // have we found the connections for a component?
            if(line.startsWith("  (   ")) {
                 Component currentComponent = components.get(components.size() - 1);
                 addNetConnection(currentComponent, line);
            }
        }

        displayComponents();

        writeNetlist(outputFilename);

        System.out.println("done");
    }


    public static void writeNetlist(String outputFilename) throws IOException {
        PrintWriter writer = new PrintWriter(outputFilename, "UTF-8");

        HashMap<String, ArrayList<HashMap<String, Component>>> allConnections = getNetlistConnections();
        Iterator<Map.Entry<String, ArrayList<HashMap<String, Component>>>> entries = allConnections.entrySet().iterator();
        while (entries.hasNext()) {

            String signalList = "";
            Map.Entry<String, ArrayList<HashMap<String, Component>>> entry = entries.next();

            for(HashMap<String, Component> rel : entry.getValue()) {
                Iterator<Map.Entry<String, Component>> relEntries = rel.entrySet().iterator();

                while (relEntries.hasNext()) {
                    Map.Entry<String, Component> relEntry = relEntries.next();
                    signalList = signalList + relEntry.getValue().getReference() + "-" + relEntry.getKey() + " ";
                }
            }
            // if net is not ? (not connected)
            if(!entry.getKey().contentEquals("?")) {
                writer.println("Signal \"" + entry.getKey() + "\"");
                writer.print( " { ");
                writer.print(signalList);
                writer.println( "} ");
            }
        }


        writer.close();
    }



    public static void displayComponents() {
        for(Component comp : components) {
            Map<String, String> kicadConnection = comp.getKicadNetConnection();
            Iterator<Map.Entry<String, String>> entries = kicadConnection.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();

                if(!entry.getValue().contentEquals("?")) {
                    System.out.println("Component = " + comp.getReference() + ", Pin = " + entry.getKey() + ", Net connection = " + entry.getValue());
                }
            }
        }
    }



    public static void addNetConnection(Component currentComponent, String line) {
        String[] lineSplit = line.split(" ");
        if(line.contains("  (    ")) {
            currentComponent.addNetConnection(lineSplit[6], lineSplit[7] );
        } else {
            currentComponent.addNetConnection(lineSplit[5], lineSplit[6] );
        }



    }

    // returns a map of components to pins

    public static Set<String> getAllNets() {
        Set<String> nets = new HashSet<String>();
        for(Component comp: components) {
            Map<String, String> netConnection = comp.getKicadNetConnection();
            Iterator<Map.Entry<String, String>> entries = netConnection.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<String, String> entry = entries.next();
                nets.add(entry.getValue());
            }
        }

        return nets;
    }


    public static HashMap<String, ArrayList<HashMap<String, Component>>> getNetlistConnections() {
        Set<String> allNets = getAllNets();

        HashMap<String, ArrayList<HashMap<String, Component>>> netToComponent = new HashMap<String, ArrayList<HashMap<String, Component>>>();
        for(String net: allNets) {
            ArrayList<HashMap<String, Component>> componentRel = new ArrayList<HashMap<String, Component>>();
            for(Component comp: components) {

                Set<String> keys = getKeysByValue(comp.getKicadNetConnection(), net);
                for(String key: keys) {
                    HashMap<String,Component> test = new HashMap<String, Component>();
                    test.put(key, comp);

                    componentRel.add(test);
                }
            }

            netToComponent.put(net, componentRel);
        }

        return netToComponent;
    }



    public static ArrayList<HashMap<String, Component>> getConnectedPins(Component component, String pin) {
        String net = component.getNetConnection(pin);
        ArrayList<HashMap<String, Component>> allComponents = new ArrayList<HashMap<String, Component>>();

            for(Component comp : components) {
                 HashMap<String, Component> thisComponent = new HashMap<String, Component>();
                 if(!comp.getReference().equals(component.getReference())) { // not this one
                     Set<String> connectedPins = getKeysByValue(comp.getKicadNetConnection(), net);
                     for(String connectedPin: connectedPins) {
                       thisComponent.put(connectedPin, component);
                     }

                     allComponents.add(thisComponent);
                 }
             }

        return allComponents;
    }


    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }


    public static Component buildComponent(String myline) {
        Component component = new Component();
        String[] lineSplit = myline.split(" ");
        component.setValue(lineSplit[VALUE_POS]);
        component.setReference(lineSplit[REFERENCE_POS]);
        component.setFootprint(lineSplit[FOOTPRINT_POS]);
        return component;
    }


}

class KicadNetConnection {
    String pin;
    String netConnection;

    String getNetConnection() {
        return netConnection;
    }

    void setNetConnection(String netConnection) {
        this.netConnection = netConnection;
    }

    String getPin() {
        return pin;
    }

    void setPin(String pin) {
        this.pin = pin;
    }
}

class Component {

    String footprint;
    String reference;
    String value;
    Map<String, String> kicadNetConnection;
    ArrayList<String> osmondNetConnection;

    Component() {
        this.kicadNetConnection = new HashMap<String, String>();
    }


    ArrayList<String> getOsmondNetConnection() {
        return osmondNetConnection;
    }

    void addToOsmondNetConnection(String connection) {
        this.osmondNetConnection.add(connection);
    }

    void addNetConnection(String pin, String netConnection) {
        kicadNetConnection.put(pin, netConnection);
    }

    String getNetConnection(String pin) {
        return kicadNetConnection.get(pin);
    }


    void setFootprint(String footprint) {
        this.footprint = footprint;
    }

    void setReference(String reference) {
        this.reference = reference;
    }

    void setValue(String value) {
        this.value = value;
    }



    String getFootprint() {
        return footprint;
    }

    String getReference() {
        return reference;
    }

    String getValue() {
        return value;
    }

    Map<String, String> getKicadNetConnection() {
        return kicadNetConnection;
    }
}