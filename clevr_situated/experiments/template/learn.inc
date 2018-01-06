## Tester
type=tester.situated id=tester data=test parser=parser validator=validator
## Filter to skip long sentences during learning
type=filter.sentence.length id=learningFilter length=%{trainingMaxSentenceLength}
## Learner
type=learner.validation.stocgrad id=stocgrad data=train genlexbeam=%{lexbeam} iter=%{gIter} validator=validator filter=learningFilter tester=tester genlex=genlex conflateParses=false errorDriven=true
