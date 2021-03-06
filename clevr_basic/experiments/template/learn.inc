## Tester
type=tester id=tester data=test parser=parser
## Filter to skip long sentences during learning
type=filter.sentence.length id=learningFilter length=%{trainingMaxSentenceLength}
## Learner
type=learner.validation.stocgrad id=stocgrad data=train genlexbeam=%{lexbeam} epochs=%{gEpochs} maxIterations=%{gMaxIterations} validator=validator filter=learningFilter tester=tester genlex=genlexDummy conflateParses=false errorDriven=true filterFactory=filterFactory
