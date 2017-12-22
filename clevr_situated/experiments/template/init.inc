## Model initializers
type=init.lex   id=initSeedLex lexicon=seedLexicon
type=scorer.lenexp id=expScorer coef=10.0 exp=1.1
type=init.lex.weights id=initSeedLexWeights lexicon=seedLexicon key=FACLEX#LEX scorer=expScorer
type=init.lex.weights id=initSeedXemeWeights lexicon=seedLexicon key=FACLEX#XEME value=10.0
type=init.weights id=initWeights file=../../../resources/init.weights
