## Prepare

````
npm install
````

## Connection settings

- Adjust settings.json 

## Test Firebase

````
npm start
````

## Check local content data

````
npm run data:c {<lang>, ...}
````

## Check remote data

````
npm run data:k {<lang>, ...}
````

## Load default data (and update)

````
npm run data:l {true}
````

## Load icons

````
npm run data:i
````

## Activate stage data

### List threshold data

````
npm run data:a
````

### Activate threshold data

````
npm run data:a {-- '<key>, ...'}
````

## Validate stage data

````
npm run data:v
````

## Propagate Favorites

````
npm run data:f
````

## Show Top Stage

````
npm run data:t
````

## Show/Update Stage without Primary Reference

````
npm run data:p {-- '<tag_key=prim_ref_tag_key>, ...'}
````

## Match & Messages

### Cleanup old messages & matches (< 1 day)

````
npm run match:c
````

## Analytics

### Initialize Postgres DB (one-time)

````
npm run pginit
````

### Start Postgres DB

````
npm run pgstart
````

### Stage Data in File

````
npm run ana:s ["data", "all", <year>, <cluster>]
````

### Activate Data File in DB

````
npm run ana:a
````

### Clear Analytics Data (Dangerous!!)

````
npm run ana:c
````

### Query Postgres

````
npm run pgsql
````

### Stop Postgres DB

````
npm run pgstart
````

## Scores

### Stage score

````
npm run score:s
````

### Activate score

````
npm run score:a
````

### Clear score

````
npm run score:c
````

## Widestage Reporting

### Start MongoDB

````
mongod
````

### Start Widestage

````
Install MongoDB
Start MongoDB 
    - npm run mongostart
Install Postgres
Start Postgres
    - npm run pgstart
Clone Widestage: git clone https://github.com/widestage/widestage.git
    - npm install
    - bower install
    - Fix in server.js line 104: 
        ipaddr = "127.0.0.1"; port = 8181;
    - npm start
    - Browser: http://127.0.0.1:8181
        - user: administrator
        - password: widestage
````