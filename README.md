



```bash
docker run \
    --name calculator-neo4j \
    -p7474:7474 -p7687:7687 \
    -d \
    -v $HOME/neo4j/data:/data \
    -v $HOME/neo4j/logs:/logs \
    -v $HOME/neo4j/import:/var/lib/neo4j/import \
    -v $HOME/neo4j/plugins:/plugins \
    --env NEO4J_AUTH=neo4j/test \
    --env NEO4J_PLUGINS='["graph-data-science"]' \
    neo4j:latest
```



```
CREATE
  (nAlice:User {name: 'Alice', seed: 42}),
  (nBridget:User {name: 'Bridget', seed: 42}),
  (nCharles:User {name: 'Charles', seed: 42}),
  (nDoug:User {name: 'Doug'}),
  (nMark:User {name: 'Mark'}),
  (nMichael:User {name: 'Michael'}),

  (nAlice)-[:LINK {weight: 1}]->(nBridget),
  (nAlice)-[:LINK {weight: 1}]->(nCharles),
  (nCharles)-[:LINK {weight: 1}]->(nBridget),

  (nAlice)-[:LINK {weight: 5}]->(nDoug),

  (nMark)-[:LINK {weight: 1}]->(nDoug),
  (nMark)-[:LINK {weight: 1}]->(nMichael),
  (nMichael)-[:LINK {weight: 1}]->(nMark);
```



```
CALL gds.graph.drop('myGraph') YIELD graphName;
CALL gds.graph.project(
    'myGraph',
    'User',
    {
        LINK: {
            orientation: 'UNDIRECTED'
        }
    },
    {
        nodeProperties: 'seed',
        relationshipProperties: 'weight'
    }
)
```



```
CALL gds.alpha.leiden.stream('myGraph', { randomSeed: 19 })
YIELD nodeId, communityId
RETURN gds.util.asNode(nodeId).name AS name, communityId
ORDER BY name ASC
```



---

```
match (s:Service) return count(s)
```

```
match (s:Service) delete s
```



```
match (s:Service {name: 'Reject_TUVOG'}) -- (o:Service)
return s, o
```

```
match (s:Service)-[:COMMON_CHANGES]-(OtherNodes)
return s, OtherNodes
```



```
CALL gds.graph.drop('commonChangesGraph', false) YIELD graphName;
CALL gds.graph.project(
    'commonChangesGraph',
    'Service',
    {
        COMMON_CHANGES: {
            orientation: 'UNDIRECTED'
        }
    },
    {
        relationshipProperties: 'weight'
    }
)
```



```
CALL gds.alpha.leiden.stream('commonChangesGraph', { 
	randomSeed: 19, 
	relationshipWeightProperty: 'weight' 
})
YIELD nodeId, communityId
RETURN gds.util.asNode(nodeId).name AS name, communityId
ORDER BY name ASC
```

```
CALL gds.alpha.leiden.stats('commonChangesGraph', { 
	randomSeed: 19,
	maxLevels: 20,
	relationshipWeightProperty: 'weight'
})
YIELD communityCount
```



```
CALL gds.louvain.stream('commonChangesGraph', { relationshipWeightProperty: 'weight' })
YIELD nodeId, communityId, intermediateCommunityIds
RETURN gds.util.asNode(nodeId).name AS name, communityId, intermediateCommunityIds
ORDER BY name ASC
```

```
CALL gds.louvain.stats('commonChangesGraph', { relationshipWeightProperty: 'weight' })
YIELD communityCount
```



```
CALL gds.labelPropagation.stream('commonChangesGraph', { relationshipWeightProperty: 'weight' })
YIELD nodeId, communityId AS Community
RETURN gds.util.asNode(nodeId).name AS Name, Community
ORDER BY Community, Name
```

```
CALL gds.labelPropagation.stats('commonChangesGraph', { relationshipWeightProperty: 'weight' })
YIELD communityCount
```



---



```
CALL gds.graph.drop('commonChangesGraph', false) YIELD graphName;
CALL gds.graph.project(
    'commonChangesGraph',
    'Service',
    {
        COMMON_CHANGES: {
            orientation: 'UNDIRECTED'
        }
    },
    {
        relationshipProperties: 'weight'
    }
)
```



```
CALL gds.alpha.leiden.write('commonChangesGraph', { 
	writeProperty: 'communityId',
	relationshipWeightProperty: 'weight'
})
YIELD communityCount, nodePropertiesWritten
```

