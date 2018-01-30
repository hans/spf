## Default scorers
type=scorer.uniform id=uniform0Scorer   class=lexEntry  weight=0.0
type=scorer.bayesian id=bayesianScorer defaultScorer=uniform0Scorer lexicon=lexicon model=model scorerTemplate=flat forceDisable=true
## Features
type=feat.logexp.coordination id=featLogexpCoordination cpp1=true cpap=true rept=true
type=feat.lex.factored id=featLex templateScale=0.0 lexemeScale=0.0 entryScorer=bayesianScorer
type=feat.lex.dynskip id=featSkip
type=feat.rules.count id=featRules scale=0.1
## Lexicon
type=lexicon.factored id=lexicon
## Actual model
type=model id=model lexicon=lexicon features=featLex,featLogexpCoordination,featRules,featSkip
## Debug model logger
type=logger.model id=loggerModel cluster=true
