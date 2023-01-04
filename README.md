# SFL-IS-mobile

## Table of contents:
1. [Description](#1-description)
2. [Structure](#2-structure)
3. [Building](#3-building)

## 1. Description

The module SFL-IS contains the implementation of the mobile android 
application for the SFL-IS project, which is intended for warehouse managers,
warehouse agents and delivery drivers. It offers an overview
of staff jobs, and in the case of agents and drivers, also scanning parcel
QR codes to complete their jobs.

## 2. Structure

The module structure is as follows:
 - The `res` directory contains the resource files needed by the module.
 - The `app/src/main/java/com/example/sfl_is` directory contains the source files:
  - The `Common.java` file contains all common data used by the module.
  - The `ui/login/` folder contains the implementation of the login implementation of the app.
  - The `ui/employee/EmployeeActivity.java` file contains the list of jobs for a particular employee.
  - The `ui/manager/ManagerActivity.java` file contains the warehouse manager's create job action.
  - The `ui/manager/employees/WarehouseActivity` file contains the list of employees working in the manager's warehouse. 
  - The `ui/scanner/ScannerActivity.java` file contains the implementation of a barcode scanner.

## 3. Building

The module `SFL-IS` should be built into an `.apk` file using Android Studio.
