# Set Card Game – Java Concurrency & Synchronization 

This repository contains my solution for **Assignment 2** of the Systems Programming course.  
The goal of the project was to implement a **multithreaded version of the Set card game** in Java, focusing on **concurrency, synchronization, and thread safety**.

---

## Overview
- Implemented the **game logic** of Set while the **GUI and input handling** were provided.  
- Designed **Dealer, Player, and Table** classes to manage:
  - Deck shuffling and card distribution.  
  - Token placement and validation of legal sets.  
  - Player penalties and scoring.  
- Used **threads** to simulate multiple players (human and computer) playing simultaneously.  
- Added **synchronization** to ensure fairness (FIFO) when multiple players attempt to claim a set.  

---

## Features
- **Multithreaded Players** – each player runs on its own thread.  
- **Dealer Thread** – controls game flow, timers, reshuffles, and scoring.  
- **Fair Synchronization** – concurrent set claims are handled in **first-come, first-served** order.  
- **Penalties & Freezes** – players are frozen after incorrect (and even correct) set claims for fairness.  
- **Configurable Gameplay** – number of players, timers, freeze durations, and more controlled via `config.properties`.  
- **Maven Build** – project structured and built using **Apache Maven**.  

---

## Skills Gained
- **Java concurrency and synchronization** (threads, locks, queues).  
- Designing **multithreaded game systems**.  
- Managing **fairness and deadlock-free synchronization**.  
- Using **Maven** for building and running Java projects.  
- Writing clean, testable, and efficient Java code.  

---

## ▶️ How to Run
Compile and run with Maven:
```bash
mvn clean compile exec:java
